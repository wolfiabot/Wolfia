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
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.db.entity.stats.ActionStats;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.PlayerStats;
import space.npstr.wolfia.db.entity.stats.TeamStats;
import space.npstr.wolfia.events.ReactionListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.IllegalGameStateException;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.utils.Operation;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    public EmbedBuilder getStatus() {
        final EmbedBuilder eb = new EmbedBuilder();
        eb.addField("Game", Games.POPCORN.textRep + " " + this.mode.textRep, true);
        if (!this.running) {
            eb.addField("", "**Game is not running**", false);
            return eb;
        }
        eb.addField("Day", Integer.toString(this.day), true);
        final long timeLeft = this.dayStarted + this.dayLengthMillis - System.currentTimeMillis();
        eb.addField("Time left", TextchatUtils.formatMillis(timeLeft), true);

        eb.addField("Living Players", String.join("\n", getLivingPlayerMentions()), true);
        final StringBuilder sb = new StringBuilder();
        getLivingWolves().forEach(w -> sb.append(Emojis.WOLF));
        eb.addField("Living wolves", sb.toString(), true);
        eb.addField("Gun holder", TextchatUtils.userAsMention(this.gunBearer), true);

        return eb;
    }

    @Override
    public void cleanUp() {
        this.timers.forEach(Thread::interrupt);
        if (this.wolfChat != null) {
            this.wolfChat.endUsage();
        }

        this.executor.shutdown();
        if (this.mode != GameMode.WILD) { //nothing to do for the wild mode
            resetRolesAndPermissions();
        }
    }

    @Override
    public synchronized void start(final long channelId, final GameMode mode, final Set<Long> innedPlayers)
            throws UserFriendlyException {
        try {//wrap into our own exceptions
            doArgumentChecksAndSet(channelId, mode, innedPlayers);
        } catch (final IllegalArgumentException e) {
            throw new UserFriendlyException(e.getMessage(), e);
        }

        doPermissionCheckAndPrepareChannel(this.mode != GameMode.WILD);


        this.day = 0;

        // - rand the characters
        randCharacters(innedPlayers);

        //get a hold of a private server...
        if (this.mode != GameMode.WILD) {
            this.wolfChat = allocatePrivateGuild();
            this.wolfChat.beginUsage(getWolvesIds());
        }

        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        //inform each player about his role
        final String inviteLink = TextchatUtils.getOrCreateInviteLink(channel);
        final String wolfchatInvite = this.wolfChat != null ? "Wolfchat: " + this.wolfChat.getInvite() + "\n" : "";
        final StringBuilder wolfteamNames = new StringBuilder("Your team is:\n");
        final String guildChannelAndInvite = String.format("Guild/Server: **%s**\nMain channel: **#%s** %s\n", //invite that may be empty
                channel.getGuild().getName(), channel.getName(), inviteLink);

        for (final Player player : this.getWolves()) {
            wolfteamNames.append("**").append(Wolfia.jda.getUserById(player.userId).getName()).append("** aka **")
                    .append(channel.getGuild().getMemberById(player.userId).getEffectiveName()).append("**\n");
        }

        for (final Player player : this.players) {
            final StringBuilder rolePm = new StringBuilder()
                    .append("Hi ").append(Wolfia.jda.getUserById(player.userId).getName()).append("!\n")
                    .append(player.alignment.rolePmBlockWW).append("\n");
            if (player.alignment == Alignments.VILLAGE) {
                rolePm.append("If you shoot a villager, you will die. If the wolves reach parity with the village, you lose.\n");
            }
            if (player.alignment == Alignments.WOLF) {
                rolePm.append("If you get shot, you will die. If all wolves get shot, you lose\n");
                rolePm.append(wolfteamNames);
                rolePm.append(wolfchatInvite);
            }
            rolePm.append(guildChannelAndInvite);

            Wolfia.handlePrivateOutputMessage(player.userId,
                    e -> Wolfia.handleOutputMessage(channel,
                            "%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                    "or unblock me, then issue `%s%s` to receive your role PM.",
                            TextchatUtils.userAsMention(player.userId), Config.PREFIX, RolePmCommand.COMMAND),
                    "%s", rolePm.toString()
            );
            this.rolePMs.put(player.userId, rolePm.toString());
        }

        final Guild g = channel.getGuild();
        //set up stats objects
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, channel.getName(),
                Games.POPCORN, this.mode.name(), this.players.size());
        final Map<Alignments, TeamStats> teams = new HashMap<>();
        for (final Player player : this.players) {
            final TeamStats team = teams.getOrDefault(player.alignment,
                    new TeamStats(this.gameStats, player.alignment, player.alignment.textRepWW, -1));
            final PlayerStats ps = new PlayerStats(team, player.userId,
                    g.getMemberById(player.userId).getEffectiveName(), player.alignment, player.role);
            this.playersStats.put(player.userId, ps);
            team.addPlayer(ps);
            teams.put(player.alignment, team);
        }
        for (final TeamStats team : teams.values()) {
            team.setTeamSize(team.getPlayers().size());
            this.gameStats.addTeam(team);
        }

        // - start the game
        Games.set(this);
        Wolfia.handleOutputMessage(Config.C.logChannelId, "%s `%s` Game started in guild **%s** `%s`, channel **#%s** `%s`, **%s %s %s** players",
                Emojis.VIDEO_GAME, TextchatUtils.toBerlinTime(System.currentTimeMillis()),
                g.getName(), g.getIdLong(), channel.getName(), channel.getIdLong(),
                Games.getInfo(this).textRep(), mode.textRep, this.players.size());
        this.running = true;
        this.gameStats.addAction(simpleAction(Wolfia.jda.getSelfUser().getIdLong(), Actions.GAMESTART, -1));
        //mention the players in the thread
        Wolfia.handleOutputMessage(channel, "Game has started!\n%s\n**%s** wolves are alive!",
                listLivingPlayers(), getLivingWolves().size());
        distributeGun();
    }

    @Override
    public boolean issueCommand(final GameCommand command, final CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException {
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
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue(null, Wolfia.defaultOnFail);
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
        Wolfia.handleOutputEmbed(channel, getStatus().build());
        Wolfia.handleOutputMessage(channel, "Day %s started! %s, you have %s minutes to shoot someone.",
                this.day, TextchatUtils.userAsMention(this.gunBearer), this.dayLengthMillis / 60000);

        if (this.mode != GameMode.WILD) {
            for (final Player player : getLivingPlayers()) {
                RoleAndPermissionUtils.grant(channel, channel.getGuild().getMemberById(player.userId),
                        Permission.MESSAGE_WRITE).queue(null, Wolfia.defaultOnFail);
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
            if (getPlayer(toBeKilled).isWolf()) {
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
        if (isGameOver(true)) {
            return; //we're done here
        }
        if (this.mode != GameMode.WILD) {
            RoleAndPermissionUtils.deny(channel, g.getMemberById(toBeKilled), Permission.MESSAGE_WRITE).queue(null, Wolfia.defaultOnFail);
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
            if (target.isWolf()) {
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

    //simplifies the giant constructor of an action by providing it with game/mode specific defaults
    @Override
    protected ActionStats simpleAction(final long actor, final Actions action, final long target) {
        final long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(),
                now, now, this.day, Phase.DAY, actor, action, target);
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
                    if (this.day != this.game.day) return;

                    Wolfia.handleOutputMessage(Popcorn.this.channelId,
                            "%s, **there is 1 minute left for you to shoot!**",
                            TextchatUtils.userAsMention(Popcorn.this.gunBearer));
                    Thread.sleep(oneMinute);
                } else {
                    Thread.sleep(this.game.dayLengthMillis);
                }
                if (this.day != this.game.day) return;

                //run this in it own thread in an independent executor,
                // because it may result in this PopcornTimer getting canceled in case it ends the game
                Popcorn.this.executor.execute(() -> {
                            try {
                                final Operation ifLegal = () -> Popcorn.this.gameStats.addAction(simpleAction(
                                        Wolfia.jda.getSelfUser().getIdLong(), Actions.MODKILL, this.game.gunBearer));
                                this.game.endDay(DayEndReason.TIMER, this.game.gunBearer, -1, ifLegal);
                            } catch (final IllegalGameStateException ignored) {
                            }
                        }
                );
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
            final Map<String, Player> options = GameUtils.mapToStrings(getLivingVillage(), Arrays.asList(Emojis.LETTERS));

            Wolfia.handleOutputEmbed(wolfchatChannel,
                    prepareGunDistributionEmbed(options, Collections.unmodifiableMap(this.votes)).build(), m -> {
                        options.keySet().forEach(emoji -> m.addReaction(emoji).queue(null, Wolfia.defaultOnFail));
                        Wolfia.jda.addEventListener(new ReactionListener(m,
                                //filter: only living wolves may vote
                                Popcorn.this::isLivingWolf,
                                //on reaction
                                reactionEvent -> {
                                    final Player p = options.get(reactionEvent.getReaction().getEmote().getName());
                                    if (p == null) return;
                                    voted(reactionEvent.getUser().getIdLong(), p.userId);
                                    m.editMessage(prepareGunDistributionEmbed(options,
                                            Collections.unmodifiableMap(this.votes)).build()).queue(null, Wolfia.defaultOnFail);
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

            final long getsGun = GameUtils.mostVoted(votesCopy, getLivingVillageIds());
            String out = "";
            if (reason == GunDistributionEndReason.TIMER) {
                out = "Time ran out!";
            } else if (reason == GunDistributionEndReason.EVERYONE_VOTED) {
                out = "Everyone has voted!";
            }
            Wolfia.handleOutputMessage(Popcorn.this.wolfChat.getChannelId(), //provided invite link may be empty
                    out + "\n@here, %s gets the %s! Game about to start/continue, get back to the main chat.\n%s",
                    Wolfia.jda.getUserById(getsGun).getName(), Emojis.GUN,
                    TextchatUtils.getOrCreateInviteLink(Wolfia.jda.getTextChannelById(Popcorn.this.channelId)));
            //give wolves 10 seconds to get back into the chat
            Popcorn.this.executor.schedule(() -> giveGun(getsGun), 10, TimeUnit.SECONDS);
        }

        private EmbedBuilder prepareGunDistributionEmbed(final Map<String, Player> livingVillage,
                                                         final Map<Long, Long> votesCopy) {
            final EmbedBuilder eb = new EmbedBuilder();
            final String mentionedWolves = String.join(", ", getLivingWolvesMentions());
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
