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

package space.npstr.wolfia.commands.ingame;

import java.util.List;
import javax.annotation.Nonnull;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;

@Command
public class VoteCountCommand extends GameCommand {

    public static final String TRIGGER = "votecount";

    protected VoteCountCommand(GameRegistry gameRegistry) {
        super(gameRegistry);
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("vc");
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + "\n#Post the current votecount.";
    }
}
