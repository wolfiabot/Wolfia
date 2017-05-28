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

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.RolePMCommand;
import space.npstr.wolfia.commands.ShootCommand;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.IGameCommand;
import space.npstr.wolfia.utils.Emojis;
import space.npstr.wolfia.utils.GameUtils;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by npstr on 22.10.2016
 */
public class Popcorn extends Game {

    private final static Logger log = LoggerFactory.getLogger(Popcorn.class);
    private final static Set<Integer> acceptedPlayerNumbers;

    static {
        final Set<Integer> foo = new HashSet<>();
        //for debugging and fucking around I guess
        foo.add(3); //1 wolf, 2 town
        foo.add(6); //2 wolf, 4 town
        foo.add(8); //3 wolf, 5 town
        foo.add(9); //3 wolf, 6 town
        foo.add(10); //4 wolf, 6 town
        foo.add(11); // the regular game 4 wolf 7 town
        acceptedPlayerNumbers = Collections.unmodifiableSet(foo);
    }


    //internal variables of an ongoing game
    private final long channelId;
    private final Set<PopcornPlayer> players = new HashSet<>();
    private boolean running = false;
    private final Set<Integer> hasDayEnded = new HashSet<>();
    private final Map<Long, String> rolePMs = new HashMap<>();

    private int day;
    private final long dayLength;
    private long dayStarted;
    private long gunBearer;


    public Popcorn(final long channelId) {
        this.channelId = channelId;
        this.day = 0;
        this.dayLength = 60 * 10 * 1000; //10 minutes
    }

    private void prepareChannel(final Set<Long> players) throws PermissionException {

//        // - ensure write access for the bot in the game channel
//        final Role botRole = Roles.getOrCreateRole(this.channel.getGuild(), Config.BOT_ROLE_NAME);
//        this.channel.getGuild().getController().addRolesToMember(this.channel.getGuild().getMemberById(Wolfia.jda.getSelfUser().getId()), botRole).complete();
//
//        Roles.grant(this.channel, botRole, Permission.MESSAGE_WRITE, true);
//
//
//        // - read only access for @everyone in the game channel
//        Roles.grant(this.channel, this.channel.getGuild().getPublicRole(), Permission.MESSAGE_WRITE, false);
//
//
//        // - write permission for the players
//        Roles.deleteRole(this.channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//        final Role playerRole = Roles.getOrCreateRole(this.channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//        Roles.grant(this.channel, playerRole, Permission.MESSAGE_WRITE, true);
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
    public Set<Integer> getAmountsOfPlayers() {
        return acceptedPlayerNumbers;
    }

    @Override
    public void start(final Set<Long> innedPlayers) {
        if (!acceptedPlayerNumbers.contains(innedPlayers.size())) {
            Wolfia.handleOutputMessage(this.channelId, "Oi mate please start this game with one of the allowed player sizes!");
            return;
        }
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        try {
            prepareChannel(innedPlayers);
        } catch (final PermissionException e) {
            log.error("Could not prepare channel {}, id: {}, due to missing permission: {}", channel.getName(),
                    channel.getId(), e.getPermission().name(), e);

            Wolfia.handleOutputMessage(this.channelId, "The bot is missing the permission %s to run the game in here.\nStart aborted.",
                    e.getPermission().name());
            return;
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
        final String villagerPrimer = "Hi %s,\nyou have randed **Villager**. Your goal is to kill all wolves, of which there are %s around. "
                + "\nIf you shoot a villager, you will die. If the wolves reach parity with the village, you lose.";
        villagers.forEach(userId -> {
            final String primer = String.format(villagerPrimer, Wolfia.jda.getUserById(userId).getName(), woofs.size());
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(this.channelId, "%s, **I cannot send you a private message**, please adjust your privacy settings or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    primer);
            this.rolePMs.put(userId, primer);
        });

        final String woofPrimer = "Hi %s,\nyou have randed **Wolf**. This is your team: %s\nYour goal is to reach parity with the village. "
                + "\nIf you get shot, you will die. If all %s get shot, you lose\n";
        final StringBuilder wolfteamNames = new StringBuilder();
        for (final long userId : woofs) {
            wolfteamNames.append("\n**").append(Wolfia.jda.getUserById(userId).getName()).append("** known as **")
                    .append(channel.getGuild().getMemberById(userId).getEffectiveName()).append("**");
        }
        woofs.forEach(userId -> {
            final String primer = String.format(woofPrimer, Wolfia.jda.getUserById(userId).getName(), wolfteamNames.toString(), Emojis.WOLF);
            Wolfia.handlePrivateOutputMessage(userId,
                    e -> Wolfia.handleOutputMessage(this.channelId, "%s, **I cannot send you a private message**, please adjust your privacy settings or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(userId), Config.PREFIX, RolePMCommand.COMMAND),
                    primer);
            this.rolePMs.put(userId, primer);
        });

        // - start the game
        this.running = true;
        //mention the players in the thread
        Wolfia.handleOutputMessage(channel, "Game has started!\n%s\n%s wolves are alive!", listLivingPlayers(), getLivingWolves().size());
        distributeGun();
    }

    private void distributeGun() {
        //essentially a rand //todo allow the wolves to decide this
        Wolfia.handleOutputMessage(this.channelId, "Randing the %s", Emojis.GUN);
        giveGun(GameUtils.rand(getLivingVillage()).userId);
    }

    private void giveGun(final long userId) {
        this.gunBearer = userId;
        Wolfia.handleOutputMessage(this.channelId, TextchatUtils.userAsMention(userId) + " has the %s !", Emojis.GUN);
        startDay();
    }

    private void startDay() {
        this.day++;
        this.dayStarted = System.currentTimeMillis();
        Wolfia.handleOutputMessage(this.channelId, "Day %s started! %s, you have %s minutes to shoot someone.",
                this.day, TextchatUtils.userAsMention(this.gunBearer), this.dayLength / 60000);

        new Thread(new PopcornTimer(this.day, this), "timer-popcorngame-" + this.day + "-" + this.channelId).start();
    }

    private synchronized void endDay(final DayEndReason reason, final long toBeKilled, final long survivor) throws IllegalGameStateException {
        //check if this is a valid call
        if (this.hasDayEnded.contains(this.day)) {
            throw new IllegalGameStateException("called endDay() for a day that has ended already");
        }
        this.hasDayEnded.add(this.day);
        Wolfia.handleOutputMessage(this.channelId, "Day %s has ended!", this.day);
        getPlayer(toBeKilled).isLiving = false;
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
                Wolfia.handleOutputMessage(this.channelId, "%s is an innocent villager! %s dies.",
                        TextchatUtils.userAsMention(survivor), TextchatUtils.userAsMention(toBeKilled));
                giveGun(survivor);
            }
        }
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
            Wolfia.handleOutputMessage(this.channelId, listLivingPlayers());
            return;
        }


        //itshappening.gif
        final PopcornPlayer target = getPlayer(targetId);

        try {
            if (target.isWolf) {
                endDay(DayEndReason.SHAT, targetId, shooterId);
            } else {
                endDay(DayEndReason.SHAT, shooterId, targetId);
            }
        } catch (final IllegalStateException e) {
            Wolfia.handleOutputMessage(this.channelId, "Too late! Time has run out.");
        }
    }


    private boolean isGameOver() {
        final Set<PopcornPlayer> livingMafia = getLivingWolves();
        final Set<PopcornPlayer> livingVillage = getLivingVillage();

        if (livingMafia.size() < 1) {
            this.running = false;
            //complete the sending of this in case a restart is queued
            Wolfia.jda.getTextChannelById(this.channelId).sendMessage("All wolves dead! Village wins. Thanks for playing.\nTeams:\n" + listTeams()).complete();
            Games.remove(this);
            return true;
        }

        if (livingMafia.size() >= livingVillage.size()) {
            this.running = false;
            //complete the sending of this in case a restart is queued
            Wolfia.jda.getTextChannelById(this.channelId).sendMessage("Parity reached! Wolves win. Thanks for playing.\nTeams:\n" + listTeams()).complete();
            Games.remove(this);
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
        return acceptedPlayerNumbers.contains(signedUp);
    }


    @Override
    public void resetRolesAndPermissions() {
//        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
//        //delete roles used by the game; the BOT_ROLE can stay
//        Roles.deleteRole(channel.getGuild(), Config.POPCORN_PLAYER_ROLE_NAME);
//
//        //reset permissions for @everyone in the game channel
//        channel.getPermissionOverride(channel.getGuild().getPublicRole()).delete().complete();
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
            getAmountsOfPlayers().forEach(i -> sb.append(i).append(" "));
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
                    this.game.endDay(DayEndReason.TIMER, this.game.gunBearer, -1);
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