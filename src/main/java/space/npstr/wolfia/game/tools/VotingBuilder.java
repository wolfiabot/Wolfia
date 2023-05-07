/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.game.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
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

    public VotingBuilder endTime(long endTime) {
        this.endTime = endTime;
        return this;
    }

    // any a '%timeleft' strings inside of this will be replaced with a formatted duration till endTime is reached
    public VotingBuilder header(String header) {
        this.header = header;
        return this;
    }

    //%command will be substituted
    public VotingBuilder notes(String notes) {
        this.notes = notes;
        return this;
    }

    public VotingBuilder unvoteEmoji(String unvoteEmoji) {
        this.unvoteEmoji = unvoteEmoji;
        return this;
    }

    public VotingBuilder possibleVoters(List<Player> possibleVoters) {
        this.possibleVoters = possibleVoters;
        return this;
    }

    public VotingBuilder possibleCandidates(List<Player> possibleCandidates) {
        this.possibleCandidates = possibleCandidates;
        return this;
    }

    public NiceEmbedBuilder getEmbed(Map<Player, Player> votes) {

        NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
        addHeader(neb, this.endTime - System.currentTimeMillis());

        List<VoteEntry> processedVotes = processVotes(votes);
        neb.addField(renderVotes("", processedVotes, true, true));

        return addNotes(neb);
    }

    public NiceEmbedBuilder getFinalEmbed(Map<Player, Player> votes, Phase phase, int cycle) {
        NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();

        List<VoteEntry> processedVotes = processVotes(votes);
        //descending order by votes received
        processedVotes.sort((o1, o2) -> o2.voters.size() - o1.voters.size());

        String title = "Final votecount for " + phase.textRep + " " + cycle + ":";
        neb.addField(renderVotes(title, processedVotes, false, false));
        return neb;
    }

    private NiceEmbedBuilder.ChunkingField renderVotes(String title, List<VoteEntry> votes, boolean renderEmojis, boolean renderZeroVotes) {
        NiceEmbedBuilder.ChunkingField votesField = new NiceEmbedBuilder.ChunkingField(title, false);

        for (VoteEntry ve : votes) {
            StringBuilder sb = new StringBuilder();
            if (!renderZeroVotes && ve.voters.isEmpty()) {
                continue;
            }
            List<String> votersNames = ve.voters.stream().map(Player::bothNamesFormatted).collect(Collectors.toList());
            if (renderEmojis) {
                sb.append(ve.emoji).append(" ");
            }
            sb.append("**").append(ve.voters.size()).append("** votes: ")
                    .append(ve.candidate.bothNamesFormatted())
                    .append("\nVoted by: ").append(ve.voters.isEmpty() ? "---" : String.join(", ", votersNames));
            votesField.add(sb.toString(), true);
        }
        StringBuilder nv = new StringBuilder();
        if (renderEmojis) {
            nv.append(this.unvoteEmoji).append(" ");
        }
        Set<Player> nonVoters = getNonVoters(votes.stream().flatMap(ve -> ve.voters.stream()).collect(Collectors.toSet()));
        nv.append("**Non-voters: **\n").append(nonVoters.stream()
                .map(Player::bothNamesFormatted)
                .collect(Collectors.joining(", ")));

        votesField.add(nv.toString());
        return votesField;
    }

    //also cleans out dead players
    private List<VoteEntry> processVotes(Map<Player, Player> votes) {
        List<VoteEntry> processedVotes = new ArrayList<>();
        for (Player candidate : this.possibleCandidates) {
            if (candidate.isDead()) {
                continue;
            }
            //who is voting for this player?
            List<Player> voters = new ArrayList<>();
            for (Map.Entry<Player, Player> entry : votes.entrySet()) {
                Player voter = entry.getKey();
                Player voted = entry.getValue();
                if (voted.equals(candidate) && voter.isAlive()) {
                    voters.add(voter);
                }
            }
            processedVotes.add(new VoteEntry(candidate.numberAsEmojis(), candidate, voters));
        }
        return processedVotes;
    }

    private Set<Player> getNonVoters(Collection<Player> voters) {
        Set<Player> nonVoters = new HashSet<>(this.possibleVoters);
        nonVoters.removeAll(voters);
        return nonVoters;
    }

    private NiceEmbedBuilder addHeader(NiceEmbedBuilder neb, long timeLeft) {
        String headerStr = this.header.replace("%timeleft", TextchatUtils.formatMillis(timeLeft));
        neb.addField("", headerStr, false);
        return neb;
    }

    private NiceEmbedBuilder addNotes(NiceEmbedBuilder neb) {
        neb.addField("", this.notes, false);
        return neb;
    }

    private static class VoteEntry {
        public final String emoji;
        public final Player candidate;
        public final Collection<Player> voters;

        public VoteEntry(String emoji, Player candidate, Collection<Player> voters) {
            this.emoji = emoji;
            this.candidate = candidate;
            this.voters = voters;
        }
    }

}
