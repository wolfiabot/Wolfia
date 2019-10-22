/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.stats.ActionStats;
import space.npstr.wolfia.domain.stats.GameStats;
import space.npstr.wolfia.domain.stats.PlayerStats;
import space.npstr.wolfia.domain.stats.TeamStats;
import space.npstr.wolfia.events.ReactionListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.game.exceptions.DayEndedAlreadyException;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.Operation;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Popcorn.class);

    //internal variables of an ongoing game
    private final List<Thread> timers = new ArrayList<>(); //keeps track of timer threads
    private int day = -1;
    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10); //10 minutes default
    private long dayStarted = -1;
    private long gunBearer = -1;

    @Override
    public void setDayLength(final Duration dayLength) {
        if (this.running) {
            throw new IllegalStateException("Cannot change day length externally while the game is running");
        }
        this.dayLengthMillis = dayLength.toMillis();
    }


    @Override
    public EmbedBuilder getStatus() {
        final NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
        neb.addField("Game", Games.POPCORN.textRep + " " + this.mode.textRep, true);
        if (!this.running) {
            neb.addField("", "**Game is not running**", false);
            return neb;
        }
        neb.addField("Day", Integer.toString(this.day), true);
        final long timeLeft = this.dayStarted + this.dayLengthMillis - System.currentTimeMillis();
        neb.addField("Time left", TextchatUtils.formatMillis(timeLeft), true);

        final NiceEmbedBuilder.ChunkingField living = new NiceEmbedBuilder.ChunkingField("Living Players", true);
        getLivingPlayers().forEach(p -> living.add(p.numberAsEmojis() + " " + p.bothNamesFormatted(), true));
        neb.addField(living);

        final StringBuilder sb = new StringBuilder();
        getLivingWolves().forEach(w -> sb.append(Emojis.WOLF));
        neb.addField("Living wolves", sb.toString(), true);

        String gunHolder = "Unknown " + Emojis.WOLFTHINK;
        try {
            gunHolder = getPlayer(this.gunBearer).bothNamesFormatted();
        } catch (final IllegalGameStateException ignored) {
            // ignored
        }
        neb.addField(Emojis.GUN + " holder", gunHolder, true);

        return neb;
    }

    @Override
    public void cleanUp() {
        this.timers.forEach(Thread::interrupt);
        if (this.wolfChat != null) {
            this.wolfChat.endUsage();
        }

        this.executor.shutdown();
        if (this.mode != GameMode.WILD) { //nothing to do for the wild mode
            resetRolesAndPermissions(true);
        }
    }

    @Override
    public synchronized void start(final long channelId, final GameMode mode, final Set<Long> innedPlayers) {
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
            this.wolfChat = allocatePrivateRoom();
            this.wolfChat.beginUsage(getWolvesIds());
        }

        final TextChannel gameChannel = fetchGameChannel();
        //inform each player about his role
        final String inviteLink = TextchatUtils.getOrCreateInviteLinkForChannel(gameChannel);
        final String wolfchatInvite = this.wolfChat != null ? "Wolfchat: " + this.wolfChat.getInvite() + "\n" : "";
        final StringBuilder wolfteamNames = new StringBuilder("Your team is:\n");
        final String guildChannelAndInvite = String.format("Guild/Server: **%s**%nMain channel: **#%s** %s%n", //invite that may be empty
                gameChannel.getGuild().getName(), gameChannel.getName(), inviteLink);

        for (final Player player : this.getWolves()) {
            wolfteamNames.append(player.bothNamesFormatted()).append("\n");
        }

        for (final Player player : this.players) {
            final StringBuilder rolePm = new StringBuilder()
                    .append("Hi ").append(player.getName()).append("!\n")
                    .append(player.alignment.rolePmBlockWW).append("\n");
            if (player.isGoodie()) {
                rolePm.append("If you shoot a villager, you will die. If the wolves reach parity with the village, you lose.\n");
            }
            if (player.isBaddie()) {
                rolePm.append("If you get shot, you will die. If all wolves get shot, you lose\n");
                rolePm.append(wolfteamNames);
                rolePm.append(wolfchatInvite);
            }
            rolePm.append(guildChannelAndInvite);

            player.setRolePm(rolePm.toString());
            player.sendMessage(rolePm.toString(),
                    e -> RestActions.sendMessage(gameChannel, String.format(
                            "%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                    "and/or unblock me, then issue `%s` to receive your role PM.",
                            player.asMention(), WolfiaConfig.DEFAULT_PREFIX + RolePmCommand.TRIGGER))
            );
        }

        final Guild g = gameChannel.getGuild();
        //set up stats objects
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, gameChannel.getName(),
                Games.POPCORN, this.mode, this.players.size());
        final Map<Alignments, TeamStats> teams = new EnumMap<>(Alignments.class);
        for (final Player player : this.players) {
            final Alignments alignment = player.alignment;
            final TeamStats team = teams.getOrDefault(alignment,
                    new TeamStats(this.gameStats, alignment, alignment.textRepWW, -1));
            final PlayerStats ps = new PlayerStats(team, player.userId,
                    player.getNick(), alignment, player.role);
            this.playersStats.put(player.userId, ps);
            team.addPlayer(ps);
            teams.put(alignment, team);
        }
        for (final TeamStats team : teams.values()) {
            team.setTeamSize(team.getPlayers().size());
            this.gameStats.addTeam(team);
        }

        // - start the game
        Games.set(this);
        String info = Games.getInfo(this).textRep();
        log.info("Game started in guild {} {}, channel #{} {}, {} {} {} players",
                g.getName(), g.getIdLong(), gameChannel.getName(), gameChannel.getIdLong(),
                info, mode.textRep, this.players.size());
        this.running = true;
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.GAMESTART, -1));
        //mention the players in the thread
        RestActions.sendMessage(gameChannel, String.format("Game has started!%n%s%n**%s** wolves are alive!",
                listLivingPlayers(), getLivingWolves().size()));
        distributeGun();
    }

    @Override
    public boolean issueCommand(@Nonnull final CommandContext context)
            throws IllegalGameStateException {
        if (context.command instanceof ShootCommand) {
            final long shooter = context.invoker.getIdLong();
            final Player target = GameUtils.identifyPlayer(this.players, context);
            if (target == null) return false;
            return shoot(shooter, target.userId);
        } else {
            context.replyWithMention("the '" + context.command.getTrigger() + "' command is not part of this game.");
            return false;
        }
    }

    private void distributeGun() {
        final TextChannel gameChannel = fetchGameChannel();
        if (this.mode == GameMode.WILD) { //essentially a rand
            RestActions.sendMessage(gameChannel, "Randing the " + Emojis.GUN);
            giveGun(GameUtils.rand(getLivingVillage()).userId);
        } else { //lets wolves do it
            for (final Player player : getLivingPlayers()) {
                RoleAndPermissionUtils.deny(gameChannel, gameChannel.getGuild().getMemberById(player.userId),
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue(null, RestActions.defaultOnFail());
            }
            new GunDistribution();
        }
    }

    private void giveGun(final long userId) {
        this.gunBearer = userId;
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.GIVEGUN, userId));
        RestActions.sendMessage(fetchGameChannel(), String.format("%s has received the %s !",
                TextchatUtils.userAsMention(userId), Emojis.GUN));
        startDay();
    }

    private void startDay() {
        this.day++;
        this.dayStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.DAYSTART, -1));
        final TextChannel channel = Launcher.getBotContext().getShardManager().getTextChannelById(this.channelId);
        if (channel != null) { //todo handle properly
            RestActions.sendMessage(channel, getStatus().build());
            RestActions.sendMessage(channel, String.format("Day %s started! %s, you have %s minutes to shoot someone.",
                    this.day, TextchatUtils.userAsMention(this.gunBearer), this.dayLengthMillis / 60000));

            if (this.mode != GameMode.WILD) {
                for (final Player player : getLivingPlayers()) {
                    RoleAndPermissionUtils.grant(channel, channel.getGuild().getMemberById(player.userId),
                            Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());
                }
            }
        }

        final Thread t = new Thread(new PopcornTimer(this.day, this),
                "timer-popcorngame-" + this.day + "-" + this.channelId);
        this.timers.add(t);
        t.start();
    }

    private void endDay(final DayEndReason reason, final long toBeKilled, final long survivor,
                        final Operation doIfLegal) throws DayEndedAlreadyException {
        synchronized (this.hasDayEnded) {
            //check if this is a valid call
            if (this.hasDayEnded.contains(this.day)) {
                throw new DayEndedAlreadyException();
            }
            this.hasDayEnded.add(this.day);
        }
        //an operation that shall only be run if the call to endDay() does not cause an DayEndedAlreadyException
        doIfLegal.execute();

        final Player killed;
        try {
            killed = getPlayer(toBeKilled);
            killed.kill();
        } catch (final IllegalGameStateException e) {
            //should not happen, but if it does, kill the game
            this.destroy(e);
            return;
        }
        this.gameStats.addAction(simpleAction(survivor, Actions.DEATH, toBeKilled));
        final TextChannel gameChannel = fetchGameChannel();
        final Guild g = gameChannel.getGuild();

        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.DAYEND, -1));
        RestActions.sendMessage(gameChannel, String.format("Day %s has ended!", this.day));

        //an operation that shall be run if the game isn't over; doing this so we can ge the output from he below if construct sent
        final LongConsumer doIfGameIsntOver;
        if (reason == DayEndReason.TIMER) {
            RestActions.sendMessage(gameChannel, String.format(
                    "%s took too long to decide who to shoot! They died and the %s will be redistributed.",
                    TextchatUtils.userAsMention(toBeKilled), Emojis.GUN));
            doIfGameIsntOver = ignored -> distributeGun();
        } else { //DayEndReason.SHAT
            if (reason != DayEndReason.SHAT) {
                log.error("You introduced a new day end reason but didn't handle it in the code.");
            }
            if (killed.isBaddie()) {
                RestActions.sendMessage(gameChannel, String.format("%s was a dirty %s!",
                        TextchatUtils.userAsMention(toBeKilled), Emojis.WOLF));
                doIfGameIsntOver = ignored -> startDay();
            } else {
                RestActions.sendMessage(gameChannel, String.format("%s is an innocent %s! %s dies.",
                        TextchatUtils.userAsMention(survivor), Emojis.COWBOY, TextchatUtils.userAsMention(toBeKilled)));
                doIfGameIsntOver = this::giveGun;
            }
        }

        //check win conditions
        if (isGameOver(true)) {
            return; //we're done here
        }
        if (this.mode != GameMode.WILD) {
            RoleAndPermissionUtils.deny(gameChannel, g.getMemberById(toBeKilled), Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());
        }
        doIfGameIsntOver.accept(survivor);
    }

    // can be called for debugging
    @SuppressWarnings("unused")
    public void evalShoot(final String shooterId, final String targetId) throws IllegalGameStateException {
        if (!Launcher.getBotContext().getWolfiaConfig().isDebug()) {
            log.error("Cant eval shoot outside of DEBUG mode");
            return;
        }
        shoot(Long.parseLong(shooterId), Long.parseLong(targetId));
    }

    private boolean shoot(final long shooterId, final long targetId) throws IllegalGameStateException {
        final TextChannel gameChannel = fetchGameChannel();
        //check various conditions for the shot being legal
        if (targetId == this.selfUserId) {
            RestActions.sendMessage(gameChannel, String.format("%s lol can't %s me.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN));
            return false;
        }
        if (shooterId == targetId) {
            RestActions.sendMessage(gameChannel, String.format("%s please don't %s yourself, that would make a big mess.",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN));
            return false;
        } else if (this.players.stream().noneMatch(p -> p.userId == shooterId)) {
            RestActions.sendMessage(gameChannel, String.format("%s shush, you're not playing in this game!",
                    TextchatUtils.userAsMention(shooterId)));
            return false;
        } else if (!isLiving(shooterId)) {
            RestActions.sendMessage(gameChannel, String.format("%s shush, you're dead!",
                    TextchatUtils.userAsMention(shooterId)));
            return false;
        } else if (shooterId != this.gunBearer) {
            RestActions.sendMessage(gameChannel, String.format("%s you do not have the %s!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN));
            return false;
        } else if (!isLiving(targetId)) {
            RestActions.sendMessage(gameChannel, String.format("%s you have to %s a living player of this game!",
                    TextchatUtils.userAsMention(shooterId), Emojis.GUN));
            RestActions.sendMessage(gameChannel, listLivingPlayers());
            return false;
        }


        //itshappening.gif
        final Player target = getPlayer(targetId);

        try {
            final Operation doIfLegal = () -> this.gameStats.addAction(simpleAction(shooterId, Actions.SHOOT, targetId));
            if (target.isBaddie()) {
                endDay(DayEndReason.SHAT, targetId, shooterId, doIfLegal);
            } else {
                endDay(DayEndReason.SHAT, shooterId, targetId, doIfLegal);
            }
            return true;
        } catch (final DayEndedAlreadyException e) {
            RestActions.sendMessage(gameChannel, "Too late! Time has run out.");
            return false;
        }
    }

    //simplifies the giant constructor of an action by providing it with game/mode specific defaults
    @Override
    protected ActionStats simpleAction(final long actor, final Actions action, final long target) {
        final long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(),
                now, now, this.day, Phase.DAY, actor, action, target, null);
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

                    RestActions.sendMessage(fetchGameChannel(), String.format(
                            "%s, **there is 1 minute left for you to shoot!**",
                            TextchatUtils.userAsMention(Popcorn.this.gunBearer)));
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
                                        Popcorn.this.selfUserId, Actions.MODKILL, this.game.gunBearer));
                                this.game.endDay(DayEndReason.TIMER, this.game.gunBearer, -1, ifLegal);
                            } catch (final DayEndedAlreadyException ignored) {
                                // ignored
                            }
                        }
                );
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private enum DayEndReason {
        TIMER, //gun bearer didn't shoot in time
        SHAT  //gun bearer shatted someone
    }

    class GunDistribution {
        private static final long TIME_TO_DISTRIBUTE_GUN_MILLIS = 1000L * 60; //1 minute
        private boolean done = false;
        private final Map<Long, Long> votes = new LinkedHashMap<>();//using linked to keep first votes at the top
        private final long startedMillis = System.currentTimeMillis();

        public GunDistribution() {
            RestActions.sendMessage(fetchGameChannel(), String.format(
                    "Wolves are distributing the %s! Please stand by, this may take up to %s",
                    Emojis.GUN, TextchatUtils.formatMillis(TIME_TO_DISTRIBUTE_GUN_MILLIS)));
            this.done = false;
            final TextChannel wolfchatChannel = Popcorn.this.fetchBaddieChannel();
            final Map<String, Player> options = GameUtils.mapToStrings(getLivingVillage(), Emojis.LETTERS);

            RestActions.sendMessage(wolfchatChannel, "Gun distribution!\n" + String.join(", ", getLivingWolvesMentions()),
                    __ -> RestActions.sendMessage(wolfchatChannel,
                            prepareGunDistributionEmbed(options, new HashMap<>(this.votes)).build(),
                            m -> {
                                options.keySet().forEach(emoji -> m.addReaction(emoji).queue(null, RestActions.defaultOnFail()));
                                m.getJDA().asBot().getShardManager().addEventListener(new ReactionListener(m,
                                        //filter: only living wolves may vote
                                        Popcorn.this::isLivingWolf,
                                        //on reaction
                                        reactionEvent -> {
                                            final Player p = options.get(reactionEvent.getReaction().getReactionEmote().getName());
                                            if (p == null) return;
                                            voted(reactionEvent.getUser().getIdLong(), p.userId);
                                            RestActions.editMessage(m, prepareGunDistributionEmbed(options,
                                                    new HashMap<>(this.votes)).build());
                                        },
                                        TIME_TO_DISTRIBUTE_GUN_MILLIS,
                                        aVoid -> endDistribution(new HashMap<>(this.votes),
                                                GunDistributionEndReason.TIMER)
                                ));
                            })
            );
        }

        //synchronized because it modifies the votes map
        private synchronized void voted(final long voter, final long candidate) {
            log.info("PrivateGuild #{}: user {} voted for user {}",
                    Popcorn.this.wolfChat.getNumber(), voter, candidate);
            this.votes.remove(voter);//remove first so there is an order by earliest vote (reinserting would not put the new vote to the end)
            this.votes.put(voter, candidate);
            //has everyone voted?
            if (this.votes.size() == getLivingWolves().size()) {
                endDistribution(new HashMap<>(this.votes), GunDistributionEndReason.EVERYONE_VOTED);
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

            final long getsGun = GameUtils.rand(GameUtils.mostVoted(votesCopy, getLivingVillageIds()));
            String out = "";
            if (reason == GunDistributionEndReason.TIMER) {
                out = "Time ran out!";
            } else if (reason == GunDistributionEndReason.EVERYONE_VOTED) {
                out = "Everyone has voted!";
            }
            String playerName = "Player Not Found";
            try {
                playerName = getPlayer(getsGun).bothNamesFormatted();
            } catch (final IllegalGameStateException ignored) {
                // ignored
            }
            final TextChannel baddieChannel = Popcorn.this.fetchBaddieChannel();
            RestActions.sendMessage(baddieChannel, //provided invite link may be empty
                    String.format("%s%n@here, %s gets the %s! Game about to start/continue, get back to the main chat.%n%s",
                            out, playerName, Emojis.GUN,
                            TextchatUtils.getOrCreateInviteLinkForChannel(baddieChannel)));
            //give wolves 10 seconds to get back into the chat
            Popcorn.this.executor.schedule(() -> giveGun(getsGun), 10, TimeUnit.SECONDS);
        }

        private EmbedBuilder prepareGunDistributionEmbed(final Map<String, Player> livingVillage,
                                                         final Map<Long, Long> votesCopy) {
            final NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
            final long timeLeft = TIME_TO_DISTRIBUTE_GUN_MILLIS - (System.currentTimeMillis() - this.startedMillis);
            neb.addField("", "You have " + TextchatUtils.formatMillis(timeLeft)
                    + " to distribute the gun.", false);
            final NiceEmbedBuilder.ChunkingField villagersField = new NiceEmbedBuilder.ChunkingField("", false);
            livingVillage.forEach((emoji, player) -> {
                //who is voting for this player to receive the gun?
                final List<String> voters = new ArrayList<>();
                for (final Map.Entry<Long, Long> entry : votesCopy.entrySet()) {
                    final long voter = entry.getKey();
                    final Long vote = entry.getValue();
                    if (vote.equals(player.userId)) {
                        voters.add(TextchatUtils.userAsMention(voter));
                    }
                }
                villagersField.add(emoji + " **" + voters.size() + "** votes: " + player.bothNamesFormatted() +
                        "\nVoted by: " + String.join(", ", voters) + "\n");
            });
            neb.addField(villagersField);
            final String info = "**Click the reactions below to decide who to give the gun. Dead wolves voting will be ignored.**";
            neb.addField("", info, false);
            return neb;
        }
    }

    private enum GunDistributionEndReason {
        TIMER, //time ran out
        EVERYONE_VOTED //all wolves have voted
    }
}
