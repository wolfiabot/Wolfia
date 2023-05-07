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

package space.npstr.wolfia.commands.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.dv8tion.jda.api.JDA;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.domain.Command;

import static java.util.Objects.requireNonNull;

@Command
public class ShardsCommand implements BaseCommand {

    private static final int SHARDS_PER_MESSAGE = 30;

    @Override
    public String getTrigger() {
        return "shards";
    }

    @Override
    public String help() {
        return "Show status of all shards.";
    }

    @Override
    public boolean execute(CommandContext context) {
        StringBuilder sb = new StringBuilder("```diff\n");
        List<String> messages = new ArrayList<>();
        List<JDA> sorted = new ArrayList<>(requireNonNull(context.getJda().getShardManager()).getShards());
        sorted.sort(Comparator.comparingInt(o -> o.getShardInfo().getShardId()));
        int i = 0;
        for (JDA jda : sorted) {
            sb.append(jda.getStatus() == JDA.Status.CONNECTED ? "+" : "-")
                    .append(" ")
                    .append(jda.getShardInfo().getShardString())
                    .append(" ")
                    .append(jda.getStatus())
                    .append(" -- Guilds: ")
                    .append(String.format("%04d", jda.getGuildCache().size()))
                    .append(" -- Users: ")
                    .append(jda.getUserCache().size())
                    .append("\n");
            if (++i % SHARDS_PER_MESSAGE == 0 || i == sorted.size()) {
                sb.append("```");
                messages.add(sb.toString());
                sb = new StringBuilder("```diff\n");
            }
        }
        messages.forEach(context::reply);
        return true;
    }
}
