/*
 * Copyright (C) 2016-2025 the original author or authors
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

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;

import static java.util.Objects.requireNonNull;

@Command
public class ReviveCommand implements BaseCommand {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReviveCommand.class);

    @Override
    public String getTrigger() {
        return "revive";
    }

    @Override
    public String help() {
        return "Revive a shard by id.";
    }

    @Override
    public boolean execute(CommandContext context) {
        if (!context.hasArguments()) {
            context.reply("No shard id provided! Say `" + WolfiaConfig.DEFAULT_PREFIX + "revive 0` for example.");
            return false;
        }

        String input = context.args[0];
        int shardId;
        try {
            shardId = Integer.parseUnsignedInt(input);
        } catch (NumberFormatException e) {
            context.reply("Your provided input `" + input + "` is no positive integer.");
            return false;
        }

        ShardManager shardManager = context.getJda().getShardManager();
        JDA jda = requireNonNull(shardManager).getShardById(shardId);
        if (jda == null) {
            context.reply("No shard with id " + shardId + " found.");
            return false;
        }
        shardManager.restart(shardId);
        log.info("Reviving shard {}", shardId);
        context.reply("Reviving shard  " + shardId);
        return true;
    }
}
