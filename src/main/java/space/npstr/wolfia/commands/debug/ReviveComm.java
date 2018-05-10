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

package space.npstr.wolfia.commands.debug;

import net.dv8tion.jda.core.JDA;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import javax.annotation.Nonnull;

/**
 * Created by napster on 19.11.17.
 */
public class ReviveComm extends BaseCommand implements IOwnerRestricted {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReviveComm.class);

    public ReviveComm(@Nonnull final String name, @Nonnull final String... aliases) {
        super(name, aliases);
    }

    @Nonnull
    @Override
    protected String help() {
        return "Revive a shard by id.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws DatabaseException {
        if (!context.hasArguments()) {
            context.reply("No shard id provided! Say `" + WolfiaConfig.DEFAULT_PREFIX + "revive 0` for example.");
            return false;
        }

        final String input = context.args[0];
        final int shardId;
        try {
            shardId = Integer.parseUnsignedInt(input);
        } catch (final NumberFormatException e) {
            context.reply("Your provided input `" + input + "` is no positive integer.");
            return false;
        }

        final JDA jda = Wolfia.getShardManager().getShardById(shardId);
        if (jda == null) {
            context.reply("No shard with id " + shardId + " found.");
            return false;
        }
        Wolfia.getShardManager().restart(shardId);
        log.info("Reviving shard {}", shardId);
        context.reply("Reviving shard  " + shardId);
        return true;
    }
}
