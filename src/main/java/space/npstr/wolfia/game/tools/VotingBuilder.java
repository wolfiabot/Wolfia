package space.npstr.wolfia.game.tools;

import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by napster on 16.07.17.
 * <p>
 * Easy vote embeds
 */
public class VotingBuilder {


    private String header = "You have **%timeleft** left to vote.";
    private long endTime;
    private String unvoteEmoji = Emojis.X;
    private List<Player> possibleVoters = Collections.emptyList();
    private List<Player> possibleCandidates = Collections.emptyList();
    private String notes = "**Use `%command` to cast a vote on a player." +
            "\nOnly your last vote will be counted.\nOnly votes by living players will be counted.**" +
            "\nUpdates every few seconds.";

    public VotingBuilder endTime(final long endTime) {
        this.endTime = endTime;
        return this;
    }

    // any a '%timeleft' strings inside of this will be replaced with a formatted duration till endTime is reached
    public VotingBuilder header(final String header) {
        this.header = header;
        return this;
    }

    //%command will be substituted
    public VotingBuilder notes(final String notes) {
        this.notes = notes;
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

    public VotingBuilder possibleCandidates(final List<Player> possibleCandidates) {
        this.possibleCandidates = possibleCandidates;
        return this;
    }

    public NiceEmbedBuilder getEmbed(final Map<Player, Player> votes) {

        NiceEmbedBuilder neb = new NiceEmbedBuilder();
        neb = addHeader(neb, this.endTime - System.currentTimeMillis());

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
        for (final Player candidate : this.possibleCandidates) {
            //who is voting for this player?
            final List<Player> voters = new ArrayList<>();
            for (final Map.Entry<Player, Player> entry : votes.entrySet()) {
                final Player voter = entry.getKey();
                final Player voted = entry.getValue();
                if (voted.equals(candidate)) {
                    voters.add(voter);
                }
            }
            processedVotes.add(new VoteEntry(candidate.numberAsEmojis(), candidate, voters));
        }
        return processedVotes;
    }

    private Set<Player> getNonVoters(final Collection<Player> voters) {
        final Set<Player> nonVoters = new HashSet<>(this.possibleVoters);
        nonVoters.removeAll(voters);
        return nonVoters;
    }

    private NiceEmbedBuilder addHeader(final NiceEmbedBuilder neb, final long timeLeft) {
        final String headerStr = this.header.replaceAll("%timeleft", TextchatUtils.formatMillis(timeLeft));
        neb.addField("", headerStr, false);
        return neb;
    }

    private NiceEmbedBuilder addNotes(final NiceEmbedBuilder neb) {
        neb.addField("", this.notes, false);
        return neb;
    }

    private static class VoteEntry {
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
