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

import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 27.05.17.
 * <p>
 * resend a role PM to a player
 */
public class RolePmCommand extends BaseCommand {

    public RolePmCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger()
                + "\n#Send your role for the ongoing game in a private message.";
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {
        final long userId = commandInfo.event.getAuthor().getIdLong();
        final long channelId = commandInfo.event.getChannel().getIdLong();

        final Game game = Games.get(channelId);
        if (game == null) {
            Wolfia.handleOutputMessage(channelId, "%s, there is no game going on in %s.",
                    TextchatUtils.userAsMention(userId), commandInfo.event.getTextChannel().getAsMention());
            return false;
        }

        if (!game.isUserPlaying(userId)) {
            Wolfia.handleOutputMessage(channelId, "%s, you aren't playing in this game.", TextchatUtils.userAsMention(userId));
            return false;
        }

        final String rolePm = game.getRolePm(userId);
        Wolfia.handlePrivateOutputMessage(userId,
                e -> Wolfia.handleOutputMessage(channelId, "%s, I cannot send you a private message, please unblock me and/or adjust your privacy settings.",
                        TextchatUtils.userAsMention(userId)),
                rolePm);
        return true;
    }
}
