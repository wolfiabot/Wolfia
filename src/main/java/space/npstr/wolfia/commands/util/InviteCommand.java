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

package space.npstr.wolfia.commands.util;


import java.util.List;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;

@Command
public class InviteCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "invite";

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("inv");
    }

    @Override
    public String help() {
        return invocation()
                + "\n#Post invite links for Wolfia and the Wolfia Lounge.";
    }

    @Override
    public boolean execute(CommandContext context) {
        context.reply(String.format("**Wolfia Bot Invite**:%n<%s>%n**Wolfia Lounge**:%n%s",
                App.INVITE_LINK, App.WOLFIA_LOUNGE_INVITE));
        return true;
    }
}
