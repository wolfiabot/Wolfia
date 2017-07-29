package space.npstr.wolfia.game.tools;

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
    private Map<String, Player> mappedEmojis;
    private String unvoteEmoji;
    private List<Player> possibleVoters;

    public VotingBuilder endTime(final long endTime) {
        this.endTime = endTime;
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

    public NiceEmbedBuilder getEmbed(final Map<Player, Player> votes) {

        NiceEmbedBuilder neb = new NiceEmbedBuilder();
        neb = addTimeLeft(neb, this.endTime - System.currentTimeMillis());

        final List<VoteEntry> processedVotes = processVotes(votes);
        neb.addField(renderVotes("", processedVotes, true, true));

        neb = addNotes(neb);
        return neb;
    }

    public NiceEmbedBuilder getFinalEmbed(final Map<Player, Player> votes, final Phase phase, final int cycle) {
        final NiceEmbedBuilder neb = new NiceEmbedBuilder();

        final List<VoteEntry> processedVotes = processVotes(votes);
        //descending order by votes received
        processedVotes.sort((o1, o2) -> o2.voters.size() - o1.voters.size());

        final String title = "Final votecount for " + phase.textRep + " " + cycle + ":";
        neb.addField(renderVotes(title, processedVotes, false, false));
        return neb;
    }

    private NiceEmbedBuilder.ChunkingField renderVotes(final String title, final List<VoteEntry> votes, final boolean renderEmojis, final boolean renderZeroVotes) {
        final NiceEmbedBuilder.ChunkingField votesField = new NiceEmbedBuilder.ChunkingField(title, false);

        for (final VoteEntry ve : votes) {
            final StringBuilder sb = new StringBuilder();
            if (!renderZeroVotes && ve.voters.isEmpty()) {
                continue;
            }
            final List<String> votersAsMentions = GameUtils.asMentions(ve.voters);
            if (renderEmojis) {
                sb.append(ve.emoji).append(" ");
            }
            sb.append("**").append(ve.voters.size()).append("** votes: ")
                    .append(ve.candidate.getBothNamesFormatted())
                    .append("\nVoted by: ").append(ve.voters.isEmpty() ? "---" : String.join(", ", votersAsMentions));
            votesField.add(sb.toString(), true);
        }
        final StringBuilder nv = new StringBuilder();
        if (renderEmojis) {
            nv.append(this.unvoteEmoji).append(" ");
        }
        final Set<Player> nonVoters = getNonVoters(votes.stream().flatMap(ve -> ve.voters.stream()).collect(Collectors.toSet()));
        nv.append("**Non-voters: **\n").append(String.join(", ", GameUtils.asMentions(nonVoters)));

        votesField.add(nv.toString());
        return votesField;
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

    private NiceEmbedBuilder addTimeLeft(final NiceEmbedBuilder neb, final long timeLeft) {
        neb.addField("", "You have **" + TextchatUtils.formatMillis(timeLeft)
                + "** left to vote.", false);
        return neb;
    }

    private NiceEmbedBuilder addNotes(final NiceEmbedBuilder neb) {
        final String info = "**Click the reactions below to vote a player." +
                "\nOnly your last vote will be counted.\nOnly votes by living players will be counted.**" +
                "\nUpdates every few seconds.";
        neb.addField("", info, false);
        return neb;
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
