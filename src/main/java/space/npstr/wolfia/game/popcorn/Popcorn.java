/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.game.popcorn;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.ReactionListener;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IGameCommand;
import space.npstr.wolfia.commands.game.RolePMCommand;
import space.npstr.wolfia.commands.game.ShootCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.ChannelSettings;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.db.entity.stats.ActionStats;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.PlayerStats;
import space.npstr.wolfia.db.entity.stats.TeamStats;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.Emojis;
import space.npstr.wolfia.utils.GameUtils;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.Operation;
import space.npstr.wolfia.utils.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.TextchatUtils;
import space.npstr.wolfia.utils.UserFriendlyException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static space.npstr.wolfia.game.GameInfo.GameMode;

/**
 * Created by npstr on 22.10.2016
 * <p>
 * The Popcorn game logic is in here.
 * This is organized after a thing that almost looks like a state machine that I drew up on my whiteboard.
 * Here's a picture:
 * https://i.imgur.com/RFbDtbm.jpg
 */
public class Popcorn extends Game {

    private static final Logger log = LoggerFactory.getLogger(Popcorn.class);

    //internal variables of an ongoing game
    private final List<Thread> timers = new ArrayList<>(); //keeps track of timer threads
    private PrivateGuild wolfChat = null;
    private int day = -1;
    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10); //10 minutes default
    private long dayStarted = -1;
    private final Set<Integer> hasDayEnded = new HashSet<>();
    private long gunBearer = -1;

    @Override
    public void setDayLength(final long dayLength, final TimeUnit timeUnit) {
        if (this.running) {
            throw new IllegalStateException("Cannot change day length externally while the game is running");
        }
        this.dayLengthMillis = timeUnit.toMillis(dayLength);
    }


    @Override
    public String getStatus() {

        final StringBuilder sb = new StringBuilder(Emojis.POPCORN + " Mafia");
        if (!this.running) {
            sb.append("\nGame is not running");
        } else {
            sb.append("\nDay: ").append(this.day);
            sb.append("\n").append(listLivingPlayers()).append("\n");
            getLivingWolves().forEach(w -> sb.append(Emojis.WOLF));
            sb.append("(").append(getLivingWolves().size()).append(") still alive.");
            sb.append("\n").append(TextchatUtils.userAsMention(this.gunBearer)).append(" is holding the ").append(Emojis.GUN);
            final long timeLeft = this.dayStarted + this.dayLengthMillis - System.currentTimeMillis();
            sb.append("\nTime left: ").append(TextchatUtils.formatMillis(timeLeft));
        }

        return sb.toString();
    }

    @Override
    public boolean isUserPlaying(final long userId) {
        return this.players.stream().anyMatch(p -> p.userId == userId);
    }

    @Override
    public void userPosted(final Message message) {
        if (!this.running) return;

        final long userId = message.getAuthor().getIdLong();
        final PlayerStats ps = this.playersStats.get(userId);
        if (ps != null) {
            ps.bumpPosts(message.getRawContent().length());
        }
    }

    @Override
    public void cleanUp() {
        this.timers.forEach(Thread::interrupt);
        if (this.wolfChat != null) {
            this.wolfChat.endUsage();
        }

        if (this.mode != GameMode.WILD) { //nothing to do for the wild mode
            resetRolesAndPermissions();
        }
    }

    //this throws a ton of possible exceptions :/
    @Override
    public synchronized void start(final long channelId, final GameMode mode, final Set<Long> innedPlayers) {
        if (this.running) {
            throw new IllegalStateException("Cannot start a game that is running already");
        }
        if (channelId <= 0 || Wolfia.jda.getTextChannelById(channelId) == null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot start a game with invalid/no channel (channelId: %s) set.", channelId)
            );
        }
        this.channelId = channelId;
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        final Guild g = channel.getGuild();
        if (!Games.getInfo(this).getSupportedModes().contains(mode)) {
            throw new IllegalArgumentException(String.format(
                    "Mode %s not supported by game %s", mode.name(), Games.POPCORN.name())
            );
        }
        this.mode = mode;

        if (!Games.getInfo(this).getAcceptablePlayerNumbers(this.mode).contains(innedPlayers.size())) {
            throw new IllegalArgumentException(String.format("There aren't enough (or too many) players signed up! " +
                    "Please use `%s%s` for more information", Config.PREFIX, StatusCommand.COMMAND));
        }

        //check permissions
        Games.getInfo(this).getRequiredPermissions(mode).forEach((scope, permission) -> {
            if (!RoleAndPermissionUtils.hasPermission(channel.getGuild().getSelfMember(), channel, scope, permission)) {
                throw new PermissionException(String.format(
                        "To run a %s game in %s mode in this channel, I need the permission to `%s` in this %s",
                        Games.POPCORN.textRep, mode.name(), permission.getName(), scope.name().toLowerCase())
                );
            }
        });

        if (mode != GameMode.WILD) {
            //is this a non-public channel, and if yes, has an existing access role be set?
            final boolean isChannelPublic = g.getPublicRole()
                    .hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ);
            if (isChannelPublic) {
                this.accessRoleId = g.getIdLong(); //public role / @everyone, guaranteed to exist
            } else {
                this.accessRoleId = DbWrapper.getEntity(channelId, ChannelSettings.class).getAccessRoleId();
                final Role accessRole = g.getRoleById(this.accessRoleId);
                if (accessRole == null) {
                    throw new UserFriendlyException(String.format(
                            "Non-public channel has been detected (`@everyone` is missing `%s` and/or `%s` permissions)." +
                                    " The chosen game and mode requires the channel to be either public, or have an access role set up." +
                                    " Talk to an admin of your server to fix this." +
                                    " Please refer to the documentation under %s",
                            Permission.MESSAGE_WRITE.getName(), Permission.MESSAGE_READ.getName(), App.WEBSITE
                    ));
                }
                if (!accessRole.hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                    throw new UserFriendlyException(String.format(
                            "The configured access role `%s` is missing `%s` and/or `%s` permissions in this channel." +
                                    " Talk to an admin of your server to fix this." +
                                    " Please refer to the documentation under %s",
                            accessRole.getName(), Permission.MESSAGE_WRITE.getName(),
                            Permission.MESSAGE_READ.getName(), App.WEBSITE
                    ));
                }
            }

            //is the bot allowed to manage permissions for this channel?
            if (!g.getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
                throw new PermissionException(String.format(
                        "To run a %s game in %s mode in this channel, I need the permission to `%s` in this channel",
                        Games.POPCORN.textRep, mode.name(), Permission.MANAGE_PERMISSIONS)
                );
            }
        }

        try {
            if (this.mode != GameMode.WILD) { //nothing to do for the wild mode
                prepareChannel(innedPlayers);
            }
        } catch (final PermissionException e) {
            log.error("Could not prepare channel {}, id: {}, due to missing permission: {}", channel.getName(),
                    channel.getId(), e.getPermission().getName(), e);
            throw new UserFriendlyException(String.format(
                    "The bot is missing the permission `%s` to run the selected game and mode in this channel.",
                    e.getPermission().getName()
            ), e);
        }

        this.day = 0;

        // - rand the roles
        final List<Long> rand = new ArrayList<>(innedPlayers);
        Collections.shuffle(rand);
        final Set<Long> woofs = new HashSet<>();
        final Set<Long> villagers = new HashSet<>();
        //todo redo this for the love of god
        //https://i.npstr.space/iyF.png
        //https://i.npstr.space/hsi.png
        if (innedPlayers.size() == 3) {
            woofs.addAll(rand.subList(0, 1));
            villagers.addAll(rand.subList(1, rand.size()));
        } else if (innedPlayers.size() == 6) {
            woofs.addAll(rand.subList(0, 2));
            villagers.addAll(rand.subList(2, rand.size()));
        } else if (innedPlayers.size() == 8) {
            woofs.addAll(rand.subList(0, 3));
            villagers.addAll(rand.subList(3, rand.size()));
        } else if (innedPlayers.size() == 9) {
            woofs.addAll(rand.subList(0, 3));
            villagers.addAll(rand.subList(3, rand.size()));
        } else if (innedPlayers.size() == 10) {
            woofs.addAll(rand.subList(0, 4));
            villagers.addAll(rand.subList(4, rand.size()));
        } else if (innedPlayers.size() == 11) {
            woofs.addAll(rand.subList(0, 4));
            villagers.addAll(rand.subList(4, rand.size()));
        }
        villagers.forEach(userId -> this.players.add(new Player(userId, false, Roles.VANILLA)));
        woofs.forEach(userId -> this.players.add(new Player(userId, true, Roles.VANILLA)));

        //get a hold of a private server...
        if (this.mode != GameMode.WILD) {
            this.wolfChat = Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.poll();
            if (this.wolfChat == null) {
                Wolfia.handleOutputMessage(channel,
                        "Acquiring a private server for the wolves...this may take a while.");
                log.error("Ran out of free private guilds. Please add moar.");
                try { //oh yeah...we are waiting till infinity if necessary
                    this.wolfChat = Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.take();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for a private server.");
                }
            }
            this.wolfChat.beginUsage(innedPlayers);
        }

        //inform each player about his role
        final String inviteLink = TextchatUtils.getOrCreateInviteLink(channel);
        final String villagerPrimer = "Hi %s,\n" +
                "you have randed **Villager** %s. Your goal is to kill all wolves, of which there are %s around.\n" +
                "If you shoot a villager, you will die. If the wolves reach parity with the village, you lose.\n" +
                "Guild/Server: **%s**\n" +
                "Main channel: **%s** %s";//invite that may be empty
        villagers.forEach(userId -> {
            final String primer = String.format(villagerPrimer, Wolfia.jda.getUserById(userId).getName(), Emojis.COWBOY,
                    woofs.size(), channel.getGuild().getName(), channel.getName(), inviteLink);
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(channel,
                            "%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                    "or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    primer);
            this.rolePMs.put(userId, primer);
        });

        String wp = "Hi %s,\n" +
                "you have randed **Wolf** %s. This is your team: %s\n" +
                "Your goal is to reach parity with the village.\n" +
                "If you get shot, you will die. If all wolves get shot, you lose\n" +
                "Guild/Server: **%s**\n" +
                "Main channel: **%s** %s";//invite that may be empty;
        if (this.wolfChat != null) {
            wp += "\nWolfchat: " + this.wolfChat.getInvite();
        }
        final String woofPrimer = wp;
        final StringBuilder wolfteamNames = new StringBuilder();
        for (final long userId : woofs) {
            wolfteamNames.append("\n**").append(Wolfia.jda.getUserById(userId).getName()).append("** aka **")
                    .append(channel.getGuild().getMemberById(userId).getEffectiveName()).append("**");
        }
        woofs.forEach(userId -> {
            final String primer = String.format(woofPrimer, Wolfia.jda.getUserById(userId).getName(), Emojis.WOLF,
                    wolfteamNames.toString(), channel.getGuild().getName(), channel.getName(), inviteLink);
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(channel,
                            "%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                    "or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    "%s", primer);
            this.rolePMs.put(userId, primer);
        });

        //set up stats objects
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, channel.getName(),
                Games.POPCORN, this.mode.name());
        final TeamStats w = new TeamStats(this.gameStats, Alignments.WOLF, "Wolves");
        woofs.forEach(userId -> {
            final PlayerStats ps = new PlayerStats(w, userId, g.getMemberById(userId).getEffectiveName(), Roles.VANILLA);
            this.playersStats.put(userId, ps);
            w.addPlayer(ps);
        });
        this.gameStats.addTeam(w);
        final TeamStats v = new TeamStats(this.gameStats, Alignments.VILLAGE, "Village");
        villagers.forEach(userId -> {
            final PlayerStats ps = new PlayerStats(v, userId, g.getMemberById(userId).getEffectiveName(), Roles.VANILLA);
            this.playersStats.put(userId, ps);
            v.addPlayer(ps);
        });
        this.gameStats.addTeam(v);

        // - start the game
        Games.set(this);
        this.running = true;
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GAMESTART, -1));
        //mention the players in the thread
        Wolfia.handleOutputMessage(channel, "Game has started!\n%s\n**%s** wolves are alive!",
                listLivingPlayers(), getLivingWolves().size());
        distributeGun();
    }

    @Override
    public boolean issueCommand(final IGameCommand command, final CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException {
        //todo resolve this smelly instanceof paradigm to something better in the future
        if (command instanceof ShootCommand) {
            final long shooter = commandInfo.event.getAuthor().getIdLong();
            final long target = commandInfo.event.getMessage().getMentionedUsers().get(0).getIdLong();
            return shoot(shooter, target);
        } else {
            Wolfia.handleOutputMessage(this.channelId, "%s, the '%s' command is not part of this game.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()), commandInfo.command);
            return false;
        }
    }

    private void distributeGun() {
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        if (this.mode == GameMode.WILD) { //essentially a rand
            Wolfia.handleOutputMessage(channel, "Randing the %s", Emojis.GUN);
            giveGun(GameUtils.rand(getLivingVillage()).userId);
        } else { //lets wolves do it
            for (final Player player : getLivingPlayers()) {
                RoleAndPermissionUtils.deny(channel, channel.getGuild().getMemberById(player.userId),
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
            }
            new GunDistribution();
        }
    }

    private void giveGun(final long userId) {
        this.gunBearer = userId;
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GIVEGUN, userId));
        Wolfia.handleOutputMessage(this.channelId, "%s has received the %s !",
                TextchatUtils.userAsMention(userId), Emojis.GUN);
        startDay();
    }

    private void startDay() {
        this.day++;
        this.dayStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.DAYSTART, -1));
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        Wolfia.handleOutputMessage(channel, getStatus());
        Wolfia.handleOutputMessage(channel, "Day %s started! %s, you have %s minutes to shoot someone.",
                this.day, TextchatUtils.userAsMention(this.gunBearer), this.dayLengthMillis / 60000);

        if (this.mode != GameMode.WILD) {
            for (final Player player : getLivingPlayers()) {
                RoleAndPermissionUtils.grant(channel, channel.getGuild().getMemberById(player.userId),
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
            }
        }

        final Thread t = new Thread(new PopcornTimer(this.day, this),
                "timer-popcorngame-" + this.day + "-" + this.channelId);
        this.timers.add(t);
        t.start();
    }

    private synchronized void endDay(final DayEndReason reason, final long toBeKilled, final long survivor,
                                     final Operation doIfLegal) throws IllegalGameStateException {
        //check if this is a valid call
        if (this.hasDayEnded.contains(this.day)) {
            throw new IllegalGameStateException("called endDay() for a day that has ended already");
        }
        //an operation that shall only be run if the call to endDay() does not cause an IllegalGameStateException
        doIfLegal.execute();

        getPlayer(toBeKilled).kill();
        this.gameStats.addAction(simpleAction(survivor, Actions.DEATH, toBeKilled));
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        final Guild g = channel.getGuild();

        this.hasDayEnded.add(this.day);
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.DAYEND, -1));
        Wolfia.handleOutputMessage(channel, "Day %s has ended!", this.day);

        //an operation that shall be run if the game isn't over; doing this so we can ge the output from he below if construct sent
        final Consumer<Long> doIfGameIsntOver;
        if (reason == DayEndReason.TIMER) {
            Wolfia.handleOutputMessage(channel,
                    "%s took too long to decide who to shoot! They died and the %s will be redistributed.",
                    TextchatUtils.userAsMention(toBeKilled), Emojis.GUN);
            doIfGameIsntOver = ignored -> distributeGun();
        } else if (reason == DayEndReason.SHAT) {
            if (getPlayer(toBeKilled).isWolf) {
                Wolfia.handleOutputMessage(channel, "%s was a dirty %s!",
                        TextchatUtils.userAsMention(toBeKilled), Emojis.WOLF);
                doIfGameIsntOver = ignored -> startDay();
            } else {
                Wolfia.handleOutputMessage(channel, "%s is an innocent %s! %s dies.",
                        TextchatUtils.userAsMention(survivor), Emojis.COWBOY, TextchatUtils.userAsMention(toBeKilled));
                doIfGameIsntOver = this::giveGun;
            }
        } else {
            throw new IllegalGameStateException("Day ended with unhandled DayEndReason: " + reason.name());
        }

        //check win conditions
        if (isGameOver()) {
            return; //we're done here
        }
        if (this.mode != GameMode.WILD) {
            RoleAndPermissionUtils.deny(channel, g.getMemberById(toBeKilled), Permission.MESSAGE_WRITE).queue();
        }
        doIfGameIsntOver.accept(survivor);
    }

    // can be called for debugging
    @SuppressWarnings("unused")
    public void evalShoot(final String shooterId, final String targetId) throws IllegalGameStateException {
        if (!Config.C.isDebug) {
            log.error("Cant eval shoot outside of DEBUG mode");
            return;
        }
        shoot(Long.valueOf(shooterId), Long.valueOf(targetId));
    }

    private boolean shoot(final long shooterId, final long targetId) throws IllegalGameStateException {
        //check various conditions for the shot being legal
        if (targetId == Wolfia.jda.getSelfUser().getIdLong()) {
            Wolfia.handleOutputMessage(this.channelId, "%s lol can't %s me.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return false;
        }
        if (shooterId == targetId) {
            Wolfia.handleOutputMessage(this.channelId, "%s please don't %s yourself, that would make a big mess.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return false;
        } else if (this.players.stream().noneMatch(p -> p.userId == shooterId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s shush, you're not playing in this game!",
                    TextchatUtils.userAsMention(shooterId));
            return false;
        } else if (!isLiving(shooterId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s shush, you're dead!",
                    TextchatUtils.userAsMention(shooterId));
            return false;
        } else if (shooterId != this.gunBearer) {
            Wolfia.handleOutputMessage(this.channelId, "%s you do not have the %s!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return false;
        } else if (!isLiving(targetId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s you have to %s a living player of this game!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            Wolfia.handleOutputMessage(this.channelId, "%s", listLivingPlayers());
            return false;
        }


        //itshappening.gif
        final Player target = getPlayer(targetId);

        try {
            final Operation doIfLegal = () -> this.gameStats.addAction(simpleAction(shooterId, Actions.SHOOT, targetId));
            if (target.isWolf) {
                endDay(DayEndReason.SHAT, targetId, shooterId, doIfLegal);
            } else {
                endDay(DayEndReason.SHAT, shooterId, targetId, doIfLegal);
            }
            return true;
        } catch (final IllegalStateException e) {
            Wolfia.handleOutputMessage(this.channelId, "Too late! Time has run out.");
            return false;
        }
    }


    /**
     * Checks whether any win conditions have been met, and reveals the game if yes
     */
    private boolean isGameOver() {
        final Set<Player> livingMafia = getLivingWolves();
        final Set<Player> livingVillage = getLivingVillage();

        boolean gameEnding = false;
        boolean villageWins = true;
        String out = "";
        if (livingMafia.size() < 1) {
            gameEnding = true;
            this.running = false;
            out = "All wolves dead! Village wins. Thanks for playing.\nTeams:\n" + listTeams();
        }
        //todo what if both conditions happen at the same time (not possible in popcorn, but if we expand the game modes?)
        if (livingMafia.size() >= livingVillage.size()) {
            gameEnding = true;
            this.running = false;
            villageWins = false;
            out = "Parity reached! Wolves win. Thanks for playing.\nTeams:\n" + listTeams();
        }

        if (gameEnding) {
            this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GAMEEND, -1));
            this.gameStats.setEndTime(System.currentTimeMillis());

            if (villageWins) {
                this.gameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.VILLAGE)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            } else {
                //woofs win
                this.gameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.WOLF)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            }
            DbWrapper.persist(this.gameStats);
            out += String.format("\nThis game's id is **%s**, you can watch its replay with `%s %s`",
                    this.gameStats.getGameId(), Config.PREFIX + ReplayCommand.COMMAND, this.gameStats.getGameId());
            cleanUp();
            // removing the game from the registry has to be the very last statement, since if a restart is queued, it
            // waits for an empty games registry
            Wolfia.handleOutputMessage(this.channelId,
                    ignoredMessage -> Games.remove(this),
                    throwable -> {
                        log.error("Failed to send last message of game #{}", this.gameStats.getGameId(), throwable);
                        Games.remove(this);
                    },
                    "%s", out);
            return true;
        }

        return false;
    }

    //simplifies the giant constructor of an action by providing it with game/mode specific defaults
    private ActionStats simpleAction(final long actor, final Actions action, final long target) {
        final long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(),
                now, now, this.day, -1, actor, action, target);
    }


    class PopcornTimer implements Runnable {

        protected final int day;
        protected final Popcorn game;

        public PopcornTimer(final int day, final Popcorn game) {
            this.day = day;
            this.game = game;
        }

        @Override
        public void run() {
            try {
                //if the day is longer than one minute, remind the gunholder about the time running out with 1 minute left
                final long oneMinute = TimeUnit.MINUTES.toMillis(1);
                if (this.game.dayLengthMillis > oneMinute) {
                    Thread.sleep(this.game.dayLengthMillis - oneMinute);
                    Wolfia.handleOutputMessage(Popcorn.this.channelId,
                            "%s, **there is 1 minute left for you to shoot!**",
                            TextchatUtils.userAsMention(Popcorn.this.gunBearer));
                    Thread.sleep(oneMinute);
                } else {
                    Thread.sleep(this.game.dayLengthMillis);
                }

                if (this.day == this.game.day) {
                    //run this in it own thread, because it may result in this PopcornTimer getting canceled in case it ends the game
                    Wolfia.executor.execute(() -> {
                                try {
                                    final Operation ifLegal = () -> Popcorn.this.gameStats.addAction(simpleAction(
                                            Wolfia.jda.getSelfUser().getIdLong(), Actions.MODKILL, this.game.gunBearer));
                                    this.game.endDay(DayEndReason.TIMER, this.game.gunBearer, -1, ifLegal);
                                } catch (final IllegalGameStateException ignored) {
                                }
                            }
                    );
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private enum DayEndReason {
        TIMER, //gun bearer didn't shoot in time
        SHAT  //gun bearer shatted someone
    }

    class GunDistribution {
        private static final long TIME_TO_DISTRIBUTE_GUN_MILLIS = 1000 * 60; //1 minute
        private boolean done = false;
        private final Map<Long, Long> votes = new LinkedHashMap<>();//using linked to keep first votes at the top
        private final long startedMillis = System.currentTimeMillis();

        public GunDistribution() {
            Wolfia.handleOutputMessage(Popcorn.this.channelId,
                    "Wolves are distributing the %s! Please stand by, this may take up to %s",
                    Emojis.GUN, TextchatUtils.formatMillis(TIME_TO_DISTRIBUTE_GUN_MILLIS));
            this.done = false;
            final long wolfchatChannelId = Popcorn.this.wolfChat.getChannelId();
            final TextChannel wolfchatChannel = Wolfia.jda.getTextChannelById(wolfchatChannelId);
            final Map<String, Player> options = new LinkedHashMap<>();//using linked to keep order of elements
            int i = 0;
            for (final Player player : getLivingVillage()) {
                //todo this has a hard limit of the size of that numbers, enforce it somewhere else before we run into an ArrayIndexOutOfBoundException
                options.put(Emojis.NUMBERS_AND_LETTERS.get(i++), player);
            }

            Wolfia.handleOutputEmbed(wolfchatChannel,
                    prepareGunDistributionEmbed(options, Collections.unmodifiableMap(this.votes)).build(), m -> {
                        options.keySet().forEach(emoji -> m.addReaction(emoji).queue());
                        Wolfia.jda.addEventListener(new ReactionListener(m,
                                //filter: only living wolves may vote
                                Popcorn.this::isLivingWolf,
                                //on reaction
                                reactionEvent -> {
                                    final Player p = options.get(reactionEvent.getReaction().getEmote().getName());
                                    if (p == null) return;
                                    voted(reactionEvent.getUser().getIdLong(), p.userId);
                                    m.editMessage(prepareGunDistributionEmbed(options,
                                            Collections.unmodifiableMap(this.votes)).build()).queue();
                                },
                                TIME_TO_DISTRIBUTE_GUN_MILLIS,
                                aVoid -> endDistribution(Collections.unmodifiableMap(this.votes),
                                        GunDistributionEndReason.TIMER)
                        ));
                    });
        }

        //synchronized because it modifies the votes map
        private synchronized void voted(final long voter, final long candidate) {
            log.info("PrivateGuild #{}: user {} voted for user {}",
                    Popcorn.this.wolfChat.getPrivateGuildNumber(), voter, candidate);
            this.votes.remove(voter);//remove first so there is an order by earliest vote (reinserting would not put the new vote to the end)
            this.votes.put(voter, candidate);
            //has everyone voted?
            if (this.votes.size() == getLivingWolves().size()) {
                endDistribution(Collections.unmodifiableMap(this.votes), GunDistributionEndReason.EVERYONE_VOTED);
            }
        }

        //synchronized because there is only one distribution allowed to happen
        private synchronized void endDistribution(final Map<Long, Long> votesCopy,
                                                  final GunDistributionEndReason reason) {
            if (this.done) {
                //ignore
                return;
            }
            this.done = true;

            //log votes
            votesCopy.forEach((voter, candidate) ->
                    Popcorn.this.gameStats.addAction(simpleAction(voter, Actions.VOTEGUN, candidate)));

            //find the target with the most votes
            //if there isn't one, the one who was voted first will get the gun (thats why we use a LinkedHashMap)
            long mostVotes = 0;
            long winningCandidate = GameUtils.rand(getLivingVillage()).userId; //default candidate is a rand
            for (final Long candidate : votesCopy.values()) {
                final long votesAmount = votesCopy.values().stream().filter(candidate::equals).count();
                if (votesAmount > mostVotes) {
                    mostVotes = votesAmount;
                    winningCandidate = candidate;
                }
            }
            final long getsGun = winningCandidate;
            String out = "";
            if (reason == GunDistributionEndReason.TIMER) {
                out = "Time ran out!";
            } else if (reason == GunDistributionEndReason.EVERYONE_VOTED) {
                out = "Everyone has voted!";
            }
            Wolfia.handleOutputMessage(Popcorn.this.wolfChat.getChannelId(),
                    out + "\n@here, %s gets the %s! Game about to start/continue, get back to the main chat.",
                    Wolfia.jda.getUserById(getsGun).getName(), Emojis.GUN);
            //give wolves 10 seconds to get back into the chat
            Wolfia.executor.schedule(() -> giveGun(getsGun), 10, TimeUnit.SECONDS);
        }

        private EmbedBuilder prepareGunDistributionEmbed(final Map<String, Player> livingVillage,
                                                         final Map<Long, Long> votesCopy) {
            final EmbedBuilder eb = new EmbedBuilder();
            final String mentionedWolves = String.join(", ", getLivingWolves().stream()
                    .map(p -> TextchatUtils.userAsMention(p.userId)).collect(Collectors.toList()));
            final long timeLeft = TIME_TO_DISTRIBUTE_GUN_MILLIS - (System.currentTimeMillis() - this.startedMillis);
            eb.addField("", mentionedWolves + " you have " + TextchatUtils.formatMillis(timeLeft)
                    + " to distribute the gun.", false);
            final StringBuilder sb = new StringBuilder();
            livingVillage.forEach((emoji, player) -> {
                //who is voting for this player to receive the gun?
                final List<String> voters = new ArrayList<>();
                for (final long voter : votesCopy.keySet()) {
                    if (votesCopy.get(voter).equals(player.userId)) {
                        voters.add(TextchatUtils.userAsMention(voter));
                    }
                }
                final Member m = Wolfia.jda.getTextChannelById(Popcorn.this.channelId).getGuild()
                        .getMemberById(player.userId);
                sb.append(emoji).append(" **").append(voters.size()).append("** votes: ")
                        .append(m.getUser().getName()).append(" aka ").append(m.getEffectiveName())
                        .append("\nVoted by: ").append(String.join(", ", voters)).append("\n");
            });
            eb.addField("", sb.toString(), false);
            final String info = "**Click the reactions below to decide who to give the gun. Dead wolves voting will be ignored.**";
            eb.addField("", info, false);
            return eb;
        }
    }

    private enum GunDistributionEndReason {
        TIMER, //time ran out
        EVERYONE_VOTED //all wolves have voted
    }
}
