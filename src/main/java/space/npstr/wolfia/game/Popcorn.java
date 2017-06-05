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

package space.npstr.wolfia.game;

import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IGameCommand;
import space.npstr.wolfia.commands.game.RolePMCommand;
import space.npstr.wolfia.commands.game.ShootCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.stats.ActionStats;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.PlayerStats;
import space.npstr.wolfia.db.entity.stats.TeamStats;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.utils.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by npstr on 22.10.2016
 */
public class Popcorn extends Game {

    private final static Logger log = LoggerFactory.getLogger(Popcorn.class);

    public enum MODE {WILD}//, CLASSIC} //todo not yet

    static {

    }


    //internal variables of an ongoing game
    private long channelId;
    private MODE mode;
    private final Set<PopcornPlayer> players = new HashSet<>();
    private volatile boolean running = false;
    private final Set<Integer> hasDayEnded = new HashSet<>();
    private final Map<Long, String> rolePMs = new HashMap<>();

    private int day;
    private long dayLength;
    private long dayStarted;
    private long gunBearer;

    //stats keeping classes
    private GameStats gameStats;
    private final Map<Long, PlayerStats> playersStats = new HashMap<>();
    private final AtomicInteger actionOrder = new AtomicInteger();


    public Popcorn() {
        this.mode = MODE.WILD;
    }

    private void prepareChannel(final Set<Long> players) throws PermissionException {

//        // - ensure write access for the bot in the game channel
//        final Role botRole = RoleUtils.getOrCreateRole(this.channel.getGuild(), Config.BOT_ROLE_NAME);
//        this.channel.getGuild().getController().addRolesToMember(this.channel.getGuild().getMemberById(Wolfia.jda.getSelfUser().getId()), botRole).complete();
//
//        RoleUtils.grant(this.channel, botRole, Permission.MESSAGE_WRITE, true);
//
//
//        // - read only access for @everyone in the game channel
//        RoleUtils.grant(this.channel, this.channel.getGuild().getPublicRole(), Permission.MESSAGE_WRITE, false);
//
//
//        // - write permission for the players
//        RoleUtils.deleteRole(this.channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//        final Role playerRole = RoleUtils.getOrCreateRole(this.channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//        RoleUtils.grant(this.channel, playerRole, Permission.MESSAGE_WRITE, true);
//
//        for (final String userId : players) {
//            this.channel.getGuild().getController().addRolesToMember(this.channel.getGuild().getMemberById(userId), playerRole).complete();
//        }
//
//
//        // - revoke writing rights on the discord server for players during the game?
//        playerRole.getManager().revokePermissions(Permission.MESSAGE_WRITE).complete();
//
    }


    /**
     * @return player numbers that this game supports
     */
    @Override
    public Set<Integer> getAcceptedPlayerNumbers() {
        return Game.ACCEPTABLE_PLAYER_NUMBERS_REGISTRY.get(Games.POPCORN);
    }

    @Override
    //callers of this might want to  catch an IllegalArgumentException, or make sure the value they send is a valid one
    //by checking getGamesModes() first
    public void setMode(final String mode) throws IllegalArgumentException {
        this.mode = MODE.valueOf(mode);
    }

    @Override
    public List<String> getPossibleModes() {
        return Arrays.stream(MODE.values()).map(Enum::name).collect(Collectors.toList());
    }

    @Override
    public synchronized boolean start(final Set<Long> innedPlayers) {
        if (this.running) {
            throw new IllegalStateException("Cannot start a game that is running already");
        }
        if (this.channelId <= 0) {
            throw new IllegalStateException("Cannot start a game with no channel set. Contact developer.");
        }

        this.day = 0;
        this.dayLength = 60 * 10 * 1000; //10 minutes

        if (!getAcceptedPlayerNumbers().contains(innedPlayers.size())) {
            Wolfia.handleOutputMessage(this.channelId, "Oi mate please start this game with one of the allowed player sizes!");
            return false;
        }
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        try {
            prepareChannel(innedPlayers);
        } catch (final PermissionException e) {
            log.error("Could not prepare channel {}, id: {}, due to missing permission: {}", channel.getName(),
                    channel.getId(), e.getPermission().name(), e);

            Wolfia.handleOutputMessage(this.channelId, "The bot is missing the permission %s to run the game in here.\nStart aborted.",
                    e.getPermission().name());
            return false;
        }


        // - rand the roles
        final List<Long> rand = new ArrayList<>(innedPlayers);
        Collections.shuffle(rand);
        final Set<Long> woofs = new HashSet<>();
        final Set<Long> villagers = new HashSet<>();
        //todo redo this for the love of god
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
        villagers.forEach(userId -> this.players.add(new PopcornPlayer(userId, false)));
        woofs.forEach(userId -> this.players.add(new PopcornPlayer(userId, true)));

        //bots aren't allowed to create group DMs which sucks; applying for white listing possible, but currently
        //source: https://discordapp.com/developers/docs/resources/channel#group-dm-add-recipient
        //workaround: echo the messages of the players into their PMs
        // - rand the gun/let mafia vote the gun
//        final GunDistributionChat gunChat = new GunDistributionChat(this, woofs, villagers);
//        Wolfia.handleOutputMessage(this.channel, "Mafia is distributing the gun. Everyone muted meanwhile.");


        //Updated stance on this: ppl may feel free to create a group chat and add the bot and issue gun distribution commands from there,
        // but the bot won't (cause it can't) create that group DM

        //inform each player about his role
        final String villagerPrimer = "Hi %s,\nyou have randed **Villager** %s. Your goal is to kill all wolves, of which there are %s around. "
                + "\nIf you shoot a villager, you will die. If the wolves reach parity with the village, you lose.";
        villagers.forEach(userId -> {
            final String primer = String.format(villagerPrimer, Wolfia.jda.getUserById(userId).getName(), Emojis.COWBOY, woofs.size());
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(this.channelId, "%s, **I cannot send you a private message**, please adjust your privacy settings or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    primer);
            this.rolePMs.put(userId, primer);
        });

        final String woofPrimer = "Hi %s,\nyou have randed **Wolf** %s. This is your team: %s\nYour goal is to reach parity with the village. "
                + "\nIf you get shot, you will die. If all wolves get shot, you lose\n";
        final StringBuilder wolfteamNames = new StringBuilder();
        for (final long userId : woofs) {
            wolfteamNames.append("\n**").append(Wolfia.jda.getUserById(userId).getName()).append("** known as **")
                    .append(channel.getGuild().getMemberById(userId).getEffectiveName()).append("**");
        }
        woofs.forEach(userId -> {
            final String primer = String.format(woofPrimer, Emojis.WOLF, Wolfia.jda.getUserById(userId).getName(), wolfteamNames.toString());
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(this.channelId, "%s, **I cannot send you a private message**, please adjust your privacy settings or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    "%s", primer);
            this.rolePMs.put(userId, primer);
        });

        //set up stats objects
        final Channel c = Wolfia.jda.getTextChannelById(this.channelId);
        final Guild g = c.getGuild();
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, c.getName(), Games.POPCORN, this.mode.name());
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
        this.running = true;
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GAMESTART, -1));
        //mention the players in the thread
        Wolfia.handleOutputMessage(channel, "Game has started!\n%s\n**%s** wolves are alive!", listLivingPlayers(), getLivingWolves().size());
        distributeGun();
        return true;
    }

    private void distributeGun() {
        //essentially a rand //todo allow the wolves to decide this
        Wolfia.handleOutputMessage(this.channelId, "Randing the %s", Emojis.GUN);
        giveGun(GameUtils.rand(getLivingVillage()).userId);
    }

    private void giveGun(final long userId) {
        this.gunBearer = userId;
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GIVEGUN, userId));
        Wolfia.handleOutputMessage(this.channelId, "%s has the %s !", TextchatUtils.userAsMention(userId), Emojis.GUN);
        startDay();
    }

    private void startDay() {
        this.day++;
        this.dayStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.DAYSTART, -1));
        Wolfia.handleOutputMessage(this.channelId, "Day %s started! %s, you have %s minutes to shoot someone.",
                this.day, TextchatUtils.userAsMention(this.gunBearer), this.dayLength / 60000);

        new Thread(new PopcornTimer(this.day, this), "timer-popcorngame-" + this.day + "-" + this.channelId).start();
    }

    private synchronized void endDay(final DayEndReason reason, final long toBeKilled, final long survivor, final Operation doIfLegal) throws IllegalGameStateException {
        //check if this is a valid call
        if (this.hasDayEnded.contains(this.day)) {
            throw new IllegalGameStateException("called endDay() for a day that has ended already");
        }
        //an operation that shall only be run if the call to endDay() does not cause an IllegalGameStateException
        doIfLegal.execute();

        getPlayer(toBeKilled).isLiving = false;
        this.gameStats.addAction(simpleAction(survivor, Actions.DEATH, toBeKilled));

        this.hasDayEnded.add(this.day);
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.DAYEND, -1));
        Wolfia.handleOutputMessage(this.channelId, "Day %s has ended!", this.day);

        //check win conditions
        if (isGameOver()) {
            return; //we're done here
        }

        if (reason == DayEndReason.TIMER) {
            Wolfia.handleOutputMessage(this.channelId,
                    "%s took too long to decide who to shat! They died and the %s will be redistributed.",
                    TextchatUtils.userAsMention(toBeKilled), Emojis.GUN);
            distributeGun();
        } else if (reason == DayEndReason.SHAT) {
            if (getPlayer(toBeKilled).isWolf) {
                Wolfia.handleOutputMessage(this.channelId, "%s was a dirty %s!",
                        TextchatUtils.userAsMention(toBeKilled), Emojis.WOLF);
                startDay();
            } else {
                Wolfia.handleOutputMessage(this.channelId, "%s is an innocent %s! %s dies.",
                        TextchatUtils.userAsMention(survivor), Emojis.COWBOY, TextchatUtils.userAsMention(toBeKilled));
                giveGun(survivor);
            }
        }
    }

    // can be called for debugging
    public void evalShoot(final String shooterId, final String targetId) throws IllegalGameStateException {
        if (!Config.C.isDebug) {
            log.error("Cant eval shoot outside of DEBUG mode");
            return;
        }
        shoot(Long.valueOf(shooterId), Long.valueOf(targetId));
    }

    private void shoot(final long shooterId, final long targetId) throws IllegalGameStateException {
        //check various conditions for the shot being legal
        if (targetId == Wolfia.jda.getSelfUser().getIdLong()) {
            Wolfia.handleOutputMessage(this.channelId, "%s lol can't %s me.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return;
        }
        if (shooterId == targetId) {
            Wolfia.handleOutputMessage(this.channelId, "%s please don't %s yourself, that would make a big mess.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return;
        } else if (this.players.stream().noneMatch(p -> p.userId == shooterId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s shush, you're not playing in this game!",
                    TextchatUtils.userAsMention(shooterId));
            return;
        } else if (!isLiving(shooterId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s shush, you're dead!",
                    TextchatUtils.userAsMention(shooterId));
            return;
        } else if (shooterId != this.gunBearer) {
            Wolfia.handleOutputMessage(this.channelId, "%s you do not have the %s!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            return;
        } else if (!isLiving(targetId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s you have to %s a living player of this game!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN);
            Wolfia.handleOutputMessage(this.channelId, "%s", listLivingPlayers());
            return;
        }


        //itshappening.gif
        final PopcornPlayer target = getPlayer(targetId);

        try {
            final Operation doIfLegal = () -> this.gameStats.addAction(simpleAction(shooterId, Actions.SHOOT, targetId));
            if (target.isWolf) {
                endDay(DayEndReason.SHAT, targetId, shooterId, doIfLegal);
            } else {
                endDay(DayEndReason.SHAT, shooterId, targetId, doIfLegal);
            }
        } catch (final IllegalStateException e) {
            Wolfia.handleOutputMessage(this.channelId, "Too late! Time has run out.");
        }
    }


    /**
     * Checks whether any win conditions have been met, and reveals the game if yes
     */
    private boolean isGameOver() {
        final Set<PopcornPlayer> livingMafia = getLivingWolves();
        final Set<PopcornPlayer> livingVillage = getLivingVillage();

        boolean gameEnding = false;
        boolean villageWins = true;
        String out = "";
        if (livingMafia.size() < 1) {
            gameEnding = true;
            out = "All wolves dead! Village wins. Thanks for playing.\nTeams:\n" + listTeams();
        }
        //todo what if both conditions happen at the same time (not possible in popcorn, but if we expand the game modes?)
        if (livingMafia.size() >= livingVillage.size()) {
            gameEnding = true;
            villageWins = false;
            out = "Parity reached! Wolves win. Thanks for playing.\nTeams:\n" + listTeams();
        }

        if (gameEnding) {
            this.running = false;
            this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GAMEEND, -1));
            this.gameStats.setEndTime(System.currentTimeMillis());

            if (villageWins) {
                this.gameStats.getStartingTeams().stream().filter(t -> t.getAlignment() == Alignments.VILLAGE).findFirst().ifPresent(t -> t.setWinner(true));
            } else {
                //woofs win
                this.gameStats.getStartingTeams().stream().filter(t -> t.getAlignment() == Alignments.WOLF).findFirst().ifPresent(t -> t.setWinner(true));
            }
            DbWrapper.persist(this.gameStats);
            out += String.format("\nThis game's id is **%s**, you can watch it's replay with `%s %s`", this.gameStats.getGameId(), Config.PREFIX + ReplayCommand.COMMAND, this.gameStats.getGameId());
            //complete the sending of this in case a restart is queued
            Wolfia.handleOutputMessage(true, this.channelId, "%s", out);
            //this has to be the last statement, since if a restart is queued, it waits for an empty games registry
            space.npstr.wolfia.game.Games.remove(this);
            return true;
        }

        return false;
    }

    private boolean isLiving(final long userId) {
        for (final PopcornPlayer p : this.players) {
            if (p.userId == userId && p.isLiving) {
                return true;
            }
        }
        return false;
    }

    private PopcornPlayer getPlayer(final long userId) throws IllegalGameStateException {
        for (final PopcornPlayer p : this.players) {
            if (p.userId == userId) {
                return p;
            }
        }
        throw new IllegalGameStateException("Requested player " + userId + " is not in the player list");
    }

    private Set<PopcornPlayer> getVillagers() {
        return this.players.stream()
                .filter(player -> !player.isWolf)
                .collect(Collectors.toSet());
    }

    private Set<PopcornPlayer> getLivingVillage() {
        return this.players.stream()
                .filter(player -> player.isLiving)
                .filter(player -> !player.isWolf)
                .collect(Collectors.toSet());
    }

    private Set<PopcornPlayer> getWolves() {
        return this.players.stream()
                .filter(player -> player.isWolf)
                .collect(Collectors.toSet());
    }

    private Set<PopcornPlayer> getLivingWolves() {
        return this.players.stream()
                .filter(player -> player.isLiving)
                .filter(player -> player.isWolf)
                .collect(Collectors.toSet());
    }

    private Set<PopcornPlayer> getLivingPlayers() {
        return this.players.stream()
                .filter(player -> player.isLiving)
                .collect(Collectors.toSet());
    }

    //do not post this before the game is over lol
    private String listTeams() {
        if (this.running) {
            log.warn("listTeams() called in a running game");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Village: ");
        getVillagers().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        sb.append("\nWolves: ");
        getWolves().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    private String listLivingPlayers() {
        final Set<PopcornPlayer> living = getLivingPlayers();
        final StringBuilder sb = new StringBuilder("Living players (**").append(living.size()).append("**) :");
        living.forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    private ActionStats simpleAction(final long actor, final Actions action, final long target) {
        final long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(), now, now, this.day, -1, actor, action, target);
    }

    @Override
    public void issueCommand(final IGameCommand command, final CommandParser.CommandContainer commandInfo) throws IllegalGameStateException {
        //todo resolve this smelly instanceof paradigm to something better in the future
        if (command instanceof ShootCommand) {
            final long shooter = commandInfo.event.getAuthor().getIdLong();
            final long target = commandInfo.event.getMessage().getMentionedUsers().get(0).getIdLong();
            shoot(shooter, target);
        } else {
            Wolfia.handleOutputMessage(this.channelId, "%s, the '%s' command is not part of this game.",
                    TextchatUtils.userAsMention(commandInfo.event.getAuthor().getIdLong()), commandInfo.command);
        }
    }

    @Override
    public boolean isUserPlaying(final long userId) {
        return this.players.stream().anyMatch(p -> p.userId == userId);
    }

    @Override
    public String getRolePm(final long userId) {
        return this.rolePMs.get(userId);
    }

    @Override
    public boolean isAcceptablePlayerCount(final int signedUp) {
        return getAcceptedPlayerNumbers().contains(signedUp);
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
    public void resetRolesAndPermissions() {
//        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
//        //delete roles used by the game; the BOT_ROLE can stay
//        RoleUtils.deleteRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//
//        //reset permissions for @everyone in the game channel
//        channel.getPermissionOverride(channel.getGuild().getPublicRole()).delete().complete();
    }

    @Override
    public void setChannelId(final long channelId) {
        this.channelId = channelId;
    }

    @Override
    public long getChannelId() {
        return this.channelId;
    }

    @Override
    public String getStatus() {

        final StringBuilder sb = new StringBuilder(Emojis.POPCORN + " Mafia");
        if (!this.running) {
            sb.append("\nGame is not running");
            sb.append("\nAmount of players needed to start: ");
            getAcceptedPlayerNumbers().forEach(i -> sb.append(i).append(" "));
        } else {
            sb.append("\nDay: ").append(this.day);
            sb.append("\n").append(listLivingPlayers()).append("\n");
            getLivingWolves().forEach(w -> sb.append(Emojis.WOLF));
            sb.append("(").append(getLivingWolves().size()).append(") still alive.");
            sb.append("\n").append(TextchatUtils.userAsMention(this.gunBearer)).append(" is holding the ").append(Emojis.GUN);
            sb.append("\nTime left: ").append(TextchatUtils.formatMillis(this.dayStarted + this.dayLength - System.currentTimeMillis()));
        }

        return sb.toString();
    }

    class PopcornTimer implements Runnable {

        final int day;
        final Popcorn game;

        public PopcornTimer(final int day, final Popcorn game) {
            this.day = day;
            this.game = game;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(this.game.dayLength);

                if (this.day == this.game.day) {
                    this.game.endDay(DayEndReason.TIMER, this.game.gunBearer, -1,
                            () -> Popcorn.this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.MODKILL, this.game.gunBearer)));
                }
            } catch (final InterruptedException e) {
                //todo handle interrupted exception properly
                Thread.currentThread().interrupt();
            } catch (final IllegalGameStateException ignored) {
                //todo decide if this can be safely ignored?
            }
        }
    }
}

class PopcornPlayer {

    final long userId;
    final boolean isWolf;
    boolean isLiving = true;

    public PopcornPlayer(final long userId, final boolean isWolf) {
        this.userId = userId;
        this.isWolf = isWolf;
    }
}

enum DayEndReason {
    TIMER, //gun bearer didn't shoot in time
    SHAT  //gun bearer shatted someone
}