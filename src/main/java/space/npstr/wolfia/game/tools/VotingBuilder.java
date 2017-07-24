package space.npstr.wolfia.game.tools;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by napster on 16.07.17.
 * <p>
 * Methods for running voting embeds
 */
public class VotingBuilder {


    private long endTime;
    private long guildId;
    private Map<String, Player> mappedEmojis;
    private String unvoteEmoji;
    private List<Player> possibleVoters;

    public VotingBuilder endTime(final long endTime) {
        this.endTime = endTime;
        return this;
    }

    //guild where the game is being played in
    public VotingBuilder guildId(final long guildId) {
        this.guildId = guildId;
        return this;
    }

    public VotingBuilder mappedEmojis(final Map<String, Player> mappedEmojis) {
        this.mappedEmojis = mappedEmojis;
        return this;
    }

    public VotingBuilder unvoteEmoji(final String unvoteEmoji) {
        this.unvoteEmoji = unvoteEmoji;
        return this;
    }

    public VotingBuilder possibleVoters(final List<Player> possibleVoters) {
        this.possibleVoters = possibleVoters;
        return this;
    }

    public EmbedBuilder getEmbed(final Map<Player, Player> votes) {

        EmbedBuilder eb = new EmbedBuilder();
        eb = addTimeLeft(eb, this.endTime - System.currentTimeMillis());

        final List<VoteEntry> processedVotes = processVotes(votes);
        final String votesStr = renderVotes(processedVotes, true, true).toString();
        eb.addField("", votesStr, false);

        eb = addNotes(eb);
        return eb;
    }

    public EmbedBuilder getFinalEmbed(final Map<Player, Player> votes, final Phase phase, final int cycle) {
        final EmbedBuilder eb = new EmbedBuilder();

        final List<VoteEntry> processedVotes = processVotes(votes);
        //descending order by votes received
        processedVotes.sort((o1, o2) -> o2.voters.size() - o1.voters.size());
        final String votesStr = renderVotes(processedVotes, false, false).toString();
        eb.addField("Final votecount for " + phase.textRep + " " + cycle + ":", votesStr, false);

        return eb;
    }

    private StringBuilder renderVotes(final List<VoteEntry> votes, final boolean renderEmojis, final boolean renderZeroVotes) {
        final StringBuilder sb = new StringBuilder();
        for (final VoteEntry ve : votes) {
            if (!renderZeroVotes && ve.voters.isEmpty()) {
                continue;
            }
            final Member candidateMember = Wolfia.jda.getGuildById(this.guildId).getMemberById(ve.candidate.userId);
            final List<String> votersAsMentions = GameUtils.asMentions(ve.voters);
            if (renderEmojis) {
                sb.append(ve.emoji).append(" ");
            }
            sb.append("**").append(ve.voters.size()).append("** votes: ")
                    .append(candidateMember.getUser().getName()).append(" aka **").append(candidateMember.getEffectiveName())
                    .append("**\nVoted by: ").append(ve.voters.isEmpty() ? "---" : String.join(", ", votersAsMentions)).append("\n");
        }
        if (renderEmojis) {
            sb.append(this.unvoteEmoji).append(" ");
        }
        final Set<Player> nonVoters = getNonVoters(votes.stream().flatMap(ve -> ve.voters.stream()).collect(Collectors.toSet()));
        sb.append("**Non-voters: **\n").append(String.join(", ", GameUtils.asMentions(nonVoters)));

        return sb;
    }

    private List<VoteEntry> processVotes(final Map<Player, Player> votes) {
        final List<VoteEntry> processedVotes = new ArrayList<>();
        for (final String emoji : this.mappedEmojis.keySet()) {
            final Player candidate = this.mappedEmojis.get(emoji);
            //who is voting for this player?
            final List<Player> voters = new ArrayList<>();
            for (final Player voter : votes.keySet()) {
                if (votes.get(voter).equals(candidate)) {
                    voters.add(voter);
                }
            }
            processedVotes.add(new VoteEntry(emoji, candidate, voters));
        }
        return processedVotes;
    }

    private Set<Player> getNonVoters(final Collection<Player> voters) {
        final Set<Player> nonVoters = new HashSet<>(this.possibleVoters);
        nonVoters.removeAll(voters);
        return nonVoters;
    }

    private EmbedBuilder addTimeLeft(final EmbedBuilder eb, final long timeLeft) {
        return eb.addField("", "You have **" + TextchatUtils.formatMillis(timeLeft)
                + "** left to vote.", false);
    }

    private EmbedBuilder addNotes(final EmbedBuilder eb) {
        final String info = "**Click the reactions below to vote a player." +
                "\nOnly your last vote will be counted.\nOnly votes by living players will be counted.**" +
                "\nUpdates every few seconds.";
        return eb.addField("", info, false);
    }

    private class VoteEntry {
        public final String emoji;
        public final Player candidate;
        public final Collection<Player> voters;

        public VoteEntry(final String emoji, final Player candidate, final Collection<Player> voters) {
            this.emoji = emoji;
            this.candidate = candidate;
            this.voters = voters;
        }
    }

}
