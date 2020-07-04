/*
 * Copyright (C) 2016-2020 the original author or authors
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

import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by napster on 27.05.17.
 * <p>
 * resend a role PM to a player
 */
@Command
public class RolePmCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "rolepm";

    private final GameRegistry gameRegistry;

    public RolePmCommand(GameRegistry gameRegistry) {
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("rpm");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Send your role for the ongoing game in a private message.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) throws IllegalGameStateException {
        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        final Game game = this.gameRegistry.get(context.textChannel);
        if (game == null) {
            context.replyWithMention("there is no game going on in here for which I could send you a role pm.");
            return false;
        }


        if (!game.isUserPlaying(context.member)) {
            context.replyWithMention("you aren't playing in this game.");
            return false;
        }

        final String rolePm = game.getRolePm(context.member);

        context.replyPrivate(rolePm, null,
                __ -> context.replyWithMention("I cannot send you a private message, please unblock me and/or adjust your privacy settings."));
        return true;
    }
}
