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

package space.npstr.wolfia.game.mafia;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.db.entities.stats.ActionStats;
import space.npstr.wolfia.db.entities.stats.GameStats;
import space.npstr.wolfia.db.entities.stats.PlayerStats;
import space.npstr.wolfia.db.entities.stats.TeamStats;
import space.npstr.wolfia.events.PrivateChannelListener;
import space.npstr.wolfia.events.UpdatingReactionListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.exceptions.DayEndedAlreadyException;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.game.tools.VotingBuilder;
import space.npstr.wolfia.utils.PeriodicTimer;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by napster on 02.07.17.
 * <p>
 * This is it, the actual werewolf/mafia game!
 */
public class Mafia extends Game {

    private static final Logger log = LoggerFactory.getLogger(Mafia.class);

    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10); //10 minutes default
    private final long nightLengthMillis = TimeUnit.MINUTES.toMillis(1); //1 minute default
    //current cycle and phase, describing n0, d1, n1, d2, n2 etc...
    private int cycle = 0;
    private Phase phase = Phase.NIGHT;
    private long phaseStarted = -1;

    private final Map<Player, Player> votes = new LinkedHashMap<>();//using linked to keep first votes at the top
    private final Map<Player, ActionStats> voteActions = new HashMap<>();

    private final Map<Player, Player> nightkillVotes = new LinkedHashMap<>();//using linked to keep first votes at the top
    private final Map<Player, ActionStats> nightKillVoteActions = new HashMap<>();
    private final Map<Player, ActionStats> nightActions = new HashMap<>();

    private Future phaseEndTimer;
    private Future phaseEndReminder;
    private final VotingBuilder votingBuilder = new VotingBuilder()
            .unvoteEmoji(Emojis.X)
            .header("Day ends in **%timeleft** with a lynch.")
            .notes(String.format("**Use `%s` to cast a vote on a player.**"
                    + "%nOnly your last vote will be counted."
                    + "%nMajority is enabled.", Config.PREFIX + CommRegistry.COMM_TRIGGER_VOTE));

    private final VotingBuilder nightKillVotingBuilder = new VotingBuilder()
            .unvoteEmoji(Emojis.X)
            .header("Night ends in **%timeleft**.")
            .notes(String.format("**Use `%s` to cast a vote on a player.**"
                    + "%nOnly your last vote will be counted.", Config.PREFIX + CommRegistry.COMM_TRIGGER_NIGHTKILL));

    @Override
    public void setDayLength(final long dayLength, final TimeUnit timeUnit) {
        if (this.running) {
            throw new IllegalStateException("Cannot change day length externally while the game is running");
        }
        this.dayLengthMillis = timeUnit.toMillis(dayLength);
    }

    @Override
    public EmbedBuilder getStatus() {
        final NiceEmbedBuilder neb = new NiceEmbedBuilder();
        neb.addField("Game", Games.MAFIA.textRep + " " + this.mode.textRep, true);
        if (!this.running) {
            neb.addField("", "**Game is not running**", false);
            return neb;
        }
        neb.addField("Phase", this.phase.textRep + " " + this.cycle, true);
        final long timeLeft = this.phaseStarted + (this.phase == Phase.DAY ? this.dayLengthMillis : this.nightLengthMillis) - System.currentTimeMillis();
        neb.addField("Time left", TextchatUtils.formatMillis(timeLeft), true);

        final NiceEmbedBuilder.ChunkingField living = new NiceEmbedBuilder.ChunkingField("Living Players", true);
        getLivingPlayers().forEach(p -> living.add(p.numberAsEmojis() + " " + p.getBothNamesFormatted(), true));
        neb.addField(living);

        final StringBuilder sb = new StringBuilder();
        getLivingWolves().forEach(w -> sb.append(Emojis.SPY));
        neb.addField("Living Mafia", sb.toString(), true);

        return neb;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void start(final long channelId, final GameInfo.GameMode mode, final Set<Long> innedPlayers)
            throws UserFriendlyException, DatabaseException {
        try {//wrap into our own exceptions
            doArgumentChecksAndSet(channelId, mode, innedPlayers);
        } catch (final IllegalArgumentException e) {
            throw new UserFriendlyException(e.getMessage(), e);
        }

        doPermissionCheckAndPrepareChannel(true); //all werewolf games are moderated

        this.cycle = 0;
        this.phase = Phase.NIGHT;

        // - rand the characters
        randCharacters(innedPlayers);

        //get a hold of a private server...
        this.wolfChat = allocatePrivateGuild();
        this.wolfChat.beginUsage(getWolvesIds());

        final TextChannel gameChannel = fetchGameChannel();
        //inform each player about his role
        final String inviteLink = TextchatUtils.getOrCreateInviteLinkForChannel(gameChannel);
        final String wolfchatInvite = this.wolfChat.getInvite();
        final StringBuilder mafiaTeamNames = new StringBuilder("Your team is:/n");
        final String guildChannelAndInvite = String.format("Guild/Server: **%s**%nMain channel: **#%s** %s%n", //invite that may be empty
                gameChannel.getGuild().getName(), gameChannel.getName(), inviteLink);

        for (final Player player : getWolves()) {
            mafiaTeamNames.append(player.getBothNamesFormatted()).append("\n");
        }

        for (final Player player : this.players) {
            final StringBuilder rolePm = new StringBuilder()
                    .append("Hi ").append(player.getName()).append("!\n")
                    .append(player.alignment.rolePmBlockMaf).append("\n")
                    .append(player.role.rolePmBlock).append("\n");
            if (player.isBaddie()) {
                rolePm.append(mafiaTeamNames);
                rolePm.append("Wolfchat: ").append(wolfchatInvite).append("\n");
            }
            rolePm.append(guildChannelAndInvite);

            player.sendMessage(rolePm.toString(),
                    e -> RestActions.sendMessage(gameChannel,
                            String.format("%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                            "and/or unblock me, then issue `%s` to receive your role PM.",
                                    player.asMention(), Config.PREFIX + CommRegistry.COMM_TRIGGER_ROLEPM))
            );
            this.rolePMs.put(player.userId, rolePm.toString());
        }


        final Guild g = gameChannel.getGuild();
        //set up stats objects
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, gameChannel.getName(),
                Games.MAFIA, this.mode.name(), this.players.size());
        final Map<Alignments, TeamStats> teams = new HashMap<>();
        for (final Player player : this.players) {
            final Alignments alignment = player.alignment;
            final TeamStats team = teams.getOrDefault(alignment,
                    new TeamStats(this.gameStats, alignment, alignment.textRepMaf, -1));
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
        DiscordLogger.getLogger().log("%s `%s` Game started in guild **%s** `%s`, channel **#%s** `%s`, **%s %s %s** players",
                Emojis.VIDEO_GAME, TextchatUtils.berlinTime(),
                g.getName(), g.getIdLong(), gameChannel.getName(), gameChannel.getIdLong(),
                Games.getInfo(this).textRep(), mode.textRep, this.players.size());
        this.running = true;
        this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.GAMESTART, -1));
        //mention the players in the thread
        RestActions.sendMessage(gameChannel, "Game has started!\n" + listLivingPlayers());

        //start the time only after the message was actually sent
        final Consumer c = aVoid -> this.executor.schedule(this::startDay, 20, TimeUnit.SECONDS);
        RestActions.sendMessage(gameChannel, "Time to read your role PMs! Day starts in 20 seconds.", c, c);
    }

    @Override
    public boolean issueCommand(final GameCommand command, @Nonnull final CommandContext context)
            throws IllegalGameStateException {
        final Player invoker;
        try {
            invoker = getPlayer(context.invoker.getIdLong());
        } catch (final IllegalGameStateException e) {
            context.replyWithMention("shush, you're not playing in this game!");
            return false;
        }
        if (invoker.isDead()) {
            context.replyWithMention("shush, you're dead!");
            return false;
        }

        if (command instanceof VoteCommand) {
            final Player candidate = GameUtils.identifyPlayer(this.players, context);
            if (candidate == null) return false;

            return vote(invoker, candidate);
        } else if (command instanceof UnvoteCommand) {
            if (context.getGuild() != null && context.getGuild().getIdLong() == this.wolfChat.getId()) {
                return nkUnvote(invoker, context);
            }

            return unvote(invoker);
        } else if (command instanceof CheckCommand) {

            if (context.channel.getType() != ChannelType.PRIVATE) {
                context.replyWithMention("checks can only be issued in private messages!");
                return false;
            }

            if (this.phase != Phase.NIGHT) {
                context.replyWithMention("checks can only be issued during the night!");
                return false;
            }

            if (invoker.role != Roles.COP) {
                context.replyWithMention("you can't issue a check when you aren't a cop!");
                return false;
            }

            final Player target = GameUtils.identifyPlayer(this.players, context);
            if (target == null) return false;

            return check(invoker, target, context);
        } else if (command instanceof VoteCountCommand) {

            //wolves asked for one, give them a votecount of their nk votes
            if (this.phase == Phase.NIGHT && context.getGuild() != null && context.getGuild().getIdLong() == this.wolfChat.getId()) {
                context.reply(this.nightKillVotingBuilder.getEmbed(new HashMap<>(this.nightkillVotes)).build());
                return true;
            }

            if (this.phase != Phase.DAY) {
                context.replyWithMention("vote counts are only shown during the day phase!");
                return false;
            }
            context.reply(this.votingBuilder.getEmbed(new HashMap<>(this.votes)).build());
            return true;

        } else if (command instanceof NightkillCommand) {
            //equivalent to the vote command m just for baddies in the night

            final Player candidate = GameUtils.identifyPlayer(this.players, context);
            if (candidate == null) return false;

            return nkVote(invoker, candidate, context);
        } else {
            context.replyWithMention("the '" + context.command.name + "' command is not part of this game.");
            return false;
        }
    }

    private boolean vote(final Player voter, final Player candidate) {

        final TextChannel gameChannel = fetchGameChannel();
        if (this.phase != Phase.DAY) {
            RestActions.sendMessage(gameChannel, voter.asMention() + ", you can only vote during the day.");
            return false;
        }

        if (candidate.isDead()) {
            RestActions.sendMessage(gameChannel, voter.asMention() + ", you can't vote for a dead player.");
            return false;
        }

        RestActions.sendMessage(gameChannel, String.format("%s votes %s for lynch.", voter.asMention(), candidate.asMention()));

        synchronized (this.votes) {
            this.votes.remove(voter);
            this.votes.put(voter, candidate);
            this.voteActions.put(voter, simpleAction(voter.userId, Actions.VOTELYNCH, candidate.userId));

            //check for majj
            final int livingPlayersCount = getLivingPlayers().size();
            final int majThreshold = (livingPlayersCount / 2);
            final long mostVotes = GameUtils.mostVotes(this.votes);
            if (mostVotes > majThreshold) {
                RestActions.sendMessage(gameChannel, "Majority was reached!");
                try {
                    endDay();
                } catch (final DayEndedAlreadyException ignored) {
                }
            }
        }
        return true;
    }

    private boolean unvote(final Player unvoter, final boolean... silent) {

        final boolean shutUp = silent.length > 0 && silent[0];
        final TextChannel gameChannel = fetchGameChannel();
        if (this.phase != Phase.DAY) {
            if (!shutUp)
                RestActions.sendMessage(gameChannel, unvoter.asMention() + ", you can only unvote during the day.");
            return false;
        }

        final Player unvoted;
        synchronized (this.votes) {
            if (this.votes.get(unvoter) == null) {
                if (!shutUp)
                    RestActions.sendMessage(gameChannel, unvoter.asMention() + ", you can't unvote if you aren't voting in the first place.");
                return false;
            }
            unvoted = this.votes.remove(unvoter);
            this.voteActions.remove(unvoter);
        }

        if (!shutUp) {
            RestActions.sendMessage(gameChannel, String.format("%s unvoted %s.",
                    unvoter.asMention(), unvoted.asMention()));
        }
        return true;
    }

    private boolean check(final Player invoker, final Player target, @Nonnull final CommandContext context) {
        if (target.isDead()) {
            context.reply("You can't check a dead player.");
            return false;
        }

        if (target.equals(invoker)) {
            context.reply("_Check yourself before you wreck yourself._\nYou can't check yourself, please check another player of the game.");
            return false;
        }

        this.nightActions.put(invoker, simpleAction(invoker.userId, Actions.CHECK, target.userId));
        context.reply("You are checking " + target.getBothNamesFormatted() + " tonight");
        return true;
    }

    //simplifies the giant constructor of an action by providing it with game/mode specific defaults
    @Override
    protected ActionStats simpleAction(final long actor, final Actions action, final long target) {
        final long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(),
                now, now, this.cycle, this.phase, actor, action, target);
    }

    private void startDay() {
        this.cycle++;
        this.phase = Phase.DAY;
        this.phaseStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.DAYSTART, -1));

        this.votes.clear();
        this.voteActions.clear();
        final List<Player> living = getLivingPlayers();
        this.votingBuilder.endTime(this.phaseStarted + this.dayLengthMillis)
                .possibleVoters(living)
                .possibleCandidates(living);

        //open channel
        final TextChannel gameChannel = fetchGameChannel();
        RestActions.sendMessage(gameChannel, String.format("Day %s started! You have %s minutes to discuss. You may vote a"
                        + " player for lynch with `%s`. You can see the current votecount with `%s`."
                        + "\nIf a player is voted by more than half the living players (majority), they will be lynched immediately!",
                this.cycle, this.dayLengthMillis / 60000, Config.PREFIX + CommRegistry.COMM_TRIGGER_VOTE,
                Config.PREFIX + CommRegistry.COMM_TRIGGER_VOTECOUNT));
        for (final Player player : living) {
            RoleAndPermissionUtils.grant(gameChannel, gameChannel.getGuild().getMemberById(player.userId),
                    Permission.MESSAGE_WRITE).queue(null, Wolfia.defaultOnFail());
        }

        //set a timer that calls endDay()
        this.phaseEndTimer = this.executor.schedule(() -> {
            try {
                this.endDay();
            } catch (final DayEndedAlreadyException ignored) {
            }
        }, this.dayLengthMillis, TimeUnit.MILLISECONDS);
        this.phaseEndReminder = this.executor.schedule(() -> RestActions.sendMessage(gameChannel, "One minute left until day end!"),
                this.dayLengthMillis - 60000, TimeUnit.MILLISECONDS);
    }

    private void endDay() throws DayEndedAlreadyException {
        synchronized (this.hasDayEnded) {
            //check if this is a valid call
            if (this.hasDayEnded.contains(this.cycle)) {
                throw new DayEndedAlreadyException();
            }
            this.hasDayEnded.add(this.cycle);
        }
        if (this.phaseEndTimer != null) this.phaseEndTimer.cancel(false);
        if (this.phaseEndReminder != null) this.phaseEndReminder.cancel(false);

        final TextChannel gameChannel = fetchGameChannel();

        final List<Player> livingPlayers = getLivingPlayers();
        //close channel
        for (final Player livingPlayer : livingPlayers) {
            RoleAndPermissionUtils.deny(gameChannel, gameChannel.getGuild().getMemberById(livingPlayer.userId),
                    Permission.MESSAGE_WRITE).queue(null, Wolfia.defaultOnFail());
        }

        this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.DAYEND, -1));
        synchronized (this.votes) {
            RestActions.sendMessage(gameChannel, this.votingBuilder.getFinalEmbed(this.votes, this.phase, this.cycle).build());
            final List<Player> lynchCandidates = GameUtils.mostVoted(this.votes, livingPlayers);
            boolean randedLynch = false;
            final Player lynchCandidate;
            if (lynchCandidates.size() > 1) {
                randedLynch = true;
                lynchCandidate = GameUtils.rand(lynchCandidates);
            } else {
                lynchCandidate = lynchCandidates.get(0);
            }

            try {
                lynchCandidate.kill();
                this.gameStats.addAction(simpleAction(-3, Actions.LYNCH, lynchCandidate.userId));
            } catch (final IllegalGameStateException | NullPointerException e) {
                //should not happen, but if it does, kill the game
                this.destroy(e);
                return;
            }

            final long votesAmount = this.votes.values().stream().filter(p -> p.userId == lynchCandidate.userId).count();
            RestActions.sendMessage(gameChannel, String.format("%s has been lynched%s with %s votes on them!\nThey were **%s %s** %s",
                    lynchCandidate.asMention(), randedLynch ? " at random due to a tie" : "", votesAmount,
                    lynchCandidate.alignment.textRepMaf, lynchCandidate.role.textRep, lynchCandidate.getCharakterEmoji()));
            this.gameStats.addActions(this.voteActions.values());
        }

        if (!isGameOver()) {
            startNight();
        }
    }

    private void postUpdatingNightMessage() {
        final String basic = "Night falls...\n";
        RestActions.sendMessage(fetchGameChannel(), basic + nightTimeLeft(),
                m -> new PeriodicTimer(
                        TimeUnit.SECONDS.toMillis(5),
                        onUpdate -> RestActions.editMessage(m, basic + nightTimeLeft()),
                        this.phaseStarted + this.nightLengthMillis - System.currentTimeMillis(),
                        onDestruction -> RestActions.editMessage(m, basic + "Dawn breaks!")
                ), Wolfia.defaultOnFail());
    }

    private String nightTimeLeft() {
        return "Time left: " + TextchatUtils.formatMillis(this.phaseStarted + this.nightLengthMillis - System.currentTimeMillis());
    }

    private void startNight() {
        this.phase = Phase.NIGHT;
        this.phaseStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.NIGHTSTART, -1));

        this.nightActions.clear();

        postUpdatingNightMessage();

        //post a voting embed for the wolfs in wolfchat
        final TextChannel wolfchatChannel = fetchBaddieChannel();

        this.nightkillVotes.clear();
        this.nightKillVoteActions.clear();

        this.nightKillVotingBuilder.endTime(this.phaseStarted + this.nightLengthMillis)
                .possibleVoters(getLivingWolves())
                .possibleCandidates(getLivingVillage());


        RestActions.sendMessage(wolfchatChannel, "Nightkill voting!\n" + String.join(", ", getLivingWolvesMentions()),
                m -> RestActions.sendMessage(wolfchatChannel, this.nightKillVotingBuilder.getEmbed(this.nightkillVotes).build(), message -> {
                    Wolfia.addEventListener(new UpdatingReactionListener(message,
                            this::isLivingWolf,
                            __ -> {
                            },//todo move away from using a reaction listener
                            this.nightLengthMillis,
                            //on destruction
                            aVoid -> {
                                message.clearReactions().queue(null, Wolfia.defaultOnFail());
                                synchronized (this.nightkillVotes) {
                                    message.editMessage(this.nightKillVotingBuilder.getFinalEmbed(this.nightkillVotes, this.phase, this.cycle).build())
                                            .queue(null, Wolfia.defaultOnFail());
                                    final Player nightKillCandidate = GameUtils.rand(GameUtils.mostVoted(this.nightkillVotes, getLivingVillage()));

                                    RestActions.sendMessage(wolfchatChannel, String.format(
                                            "\n@here, %s will be killed! Game about to start/continue, get back to the main chat.\n%s",
                                            nightKillCandidate.getBothNamesFormatted(),
                                            TextchatUtils.getOrCreateInviteLinkForChannel(Wolfia.getTextChannelById(this.channelId))));
                                    this.gameStats.addActions(this.nightKillVoteActions.values());

                                    endNight(nightKillCandidate);
                                }
                            },
                            //update every few seconds
                            TimeUnit.SECONDS.toMillis(10),
                            aVoid -> message.editMessage(this.nightKillVotingBuilder.getEmbed(this.nightkillVotes).build())
                                    .queue(null, Wolfia.defaultOnFail())
                    ));
                }, Wolfia.defaultOnFail()), Wolfia.defaultOnFail()
        );


        //notify other roles of their possible night actions

        for (final Player p : getLivingPlayers()) {

            //cop
            if (p.role == Roles.COP) {
                final EmbedBuilder livingPlayersWithNumbers = listLivingPlayersWithNumbers(p);
                final String out = String.format("**You are a cop. Use `%s [name or number]` to check the alignment of a player.**%n" +
                                "You will receive the result at the end of the night for the last submitted target. " +
                                "If you do not submit a check, it will be randed.",
                        Config.PREFIX + CommRegistry.COMM_TRIGGER_CHECK);
                livingPlayersWithNumbers.addField("", out, false);
                final Collection<Long> randCopTargets = getLivingPlayerIds();
                randCopTargets.remove(p.userId);//dont randomly check himself
                this.nightActions.put(p, simpleAction(p.userId, Actions.CHECK, GameUtils.rand(randCopTargets)));//preset a random action
                p.sendMessage(livingPlayersWithNumbers.build(), Wolfia.defaultOnFail());
                new PrivateChannelListener(p.userId, this.nightLengthMillis, this, new CheckCommand("check"));//todo uniform listeners
            }
        }
    }

    private boolean nkVote(final Player voter, final Player nightkillVote, @Nonnull final CommandContext context) {

        if (this.phase != Phase.NIGHT) {
            context.replyWithMention("you can only vote during the night.");
            return false;
        }

        if (nightkillVote.isDead()) {
            context.replyWithMention("you can't vote a dead player for nightkill.");
            return false;
        }

        if (nightkillVote.isBaddie()) { //this needs to be revisited if multiple baddie faction become a thing
            context.replyWithMention("you can't vote to nightkill a fellow mafioso.");
            return false;
        }

        context.reply(String.format("%s votes %s for nightkill.", voter.asMention(), nightkillVote.asMention()));

        synchronized (this.nightkillVotes) {
            this.nightkillVotes.remove(voter);
            this.nightkillVotes.put(voter, nightkillVote);
            this.nightKillVoteActions.put(voter, simpleAction(voter.userId, Actions.VOTENIGHTKILL, nightkillVote.userId));
        }
        return true;
    }

    private boolean nkUnvote(final Player unvoter, @Nonnull final CommandContext context) {
        if (this.phase != Phase.NIGHT) {
            context.replyWithMention("you can only unvote during the night.");
            return false;
        }

        final Player unvoted;
        synchronized (this.nightkillVotes) {
            if (this.nightkillVotes.get(unvoter) == null) {
                context.replyWithMention("you can't unvote if you aren't voting in the first place.");
                return false;
            }
            unvoted = this.nightkillVotes.remove(unvoter);
            this.nightKillVoteActions.remove(unvoter);
        }

        context.reply(String.format("%s unvoted %s.", unvoter.asMention(), unvoted.asMention()));
        return true;
    }

    @SuppressWarnings("unchecked")
    private void endNight(final Player nightKillCandidate) {

        this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.NIGHTEND, -1));
        try {
            nightKillCandidate.kill();
            this.gameStats.addAction(simpleAction(-2, Actions.DEATH, nightKillCandidate.userId));
        } catch (final IllegalGameStateException e) {
            //should not happen, but if it does, kill the game
            this.destroy(e);
            return;
        }

        for (final ActionStats nightAction : this.nightActions.values()) {
            if (nightAction.getActionType() == Actions.CHECK) {
                try {
                    final Player checker = getPlayer(nightAction.getActor());
                    final Player checked = getPlayer(nightAction.getTarget());
                    checker.sendMessage(String.format("%s, you checked %s on night %s. Their alignment is **%s**",
                            checker.asMention(), checked.getBothNamesFormatted(), this.cycle,
                            checked.alignment.textRepMaf), Wolfia.defaultOnFail());
                    nightAction.setTimeStampHappened(System.currentTimeMillis());
                    this.gameStats.addAction(nightAction);
                } catch (final IllegalGameStateException e) {
                    log.error("Checked player {} not a player of the ongoing game in {}.", nightAction.getTarget(), this.channelId);
                }
            } else {
                log.error("Unsupported night action encountered: " + nightAction.getActionType());
            }
        }

        final TextChannel gameChannel = fetchGameChannel();
        RestActions.sendMessage(gameChannel, String.format("%s has died during the night!\nThey were **%s %s** %s",
                nightKillCandidate.asMention(), nightKillCandidate.alignment.textRepMaf,
                nightKillCandidate.role.textRep, nightKillCandidate.getCharakterEmoji()));

        if (!isGameOver()) {
            //start the timer only after the message has actually been sent
            final Consumer c = aVoid -> this.executor.schedule(this::startDay, 10, TimeUnit.SECONDS);
            RestActions.sendMessage(gameChannel, String.format("Day starts in 10 seconds.\n%s",
                    String.join(", ", getLivingPlayerMentions())),
                    c, c);
        }
    }
}
