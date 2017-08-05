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

package space.npstr.wolfia.commands.game;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;

/**
 * Created by npstr on 23.08.2016
 */
public class OutCommand extends BaseCommand {

    public static final String COMMAND = "out";


    @Override
    public String help() {
        return Config.PREFIX + COMMAND + " [@user]"
                + "\n#Remove you from the current signup list. Moderators can out other players by mentioning them.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        final SetupEntity setup = DbWrapper.getOrCreateEntity(commandInfo.event.getChannel().getIdLong(), SetupEntity.class);
        //is this a forced out of a player by an moderator or the bot owner?
        if (commandInfo.event.getMessage().getMentionedUsers().size() > 0) {
            final Member invoker = commandInfo.event.getMember();
            final TextChannel channel = commandInfo.event.getTextChannel();
            if (!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) && !App.isOwner(invoker)) {
                Wolfia.handleOutputMessage(channel, "%s, you need to have the MESSAGE_MANAGE permission for this channel to be able to out players.", invoker.getAsMention());
                return false;
            } else {
                commandInfo.event.getMessage().getMentionedUsers().forEach(u -> setup.outUser(u.getIdLong()));
                setup.postStatus();
                return true;
            }
        } else {
            //handling a regular out
            if (setup.outUser(commandInfo.event.getAuthor().getIdLong())) {
                setup.postStatus();
                return true;
            }
        }
        return false;
    }

}
