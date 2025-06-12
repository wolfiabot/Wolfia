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

package space.npstr.wolfia.domain.setup;

import java.util.List;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.Game;

import static java.util.Objects.requireNonNull;

/**
 * This command should display the status of whatever is happening in a channel currently
 * <p>
 * Is there a game running, whats it's state?
 * If not, is there a setup created for this channel, whats the status here, inned players etc?
 */
@Command
public class StatusCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "status";

    private final GameSetupService gameSetupService;
    private final GameSetupRender render;
    private final GameRegistry gameRegistry;

    public StatusCommand(GameSetupService gameSetupService, GameSetupRender render, GameRegistry gameRegistry) {
        this.gameSetupService = gameSetupService;
        this.render = render;
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public List<String> getAliases() {
        return List.of("st");
    }

    @Override
    public String help() {
        return invocation() + "\n#Post the current game status or sign up list.";
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean execute(CommandContext commandContext) {
        //this command may be called from any channel. if its a private channel, look for ongoing games of the invoker

        GuildCommandContext context = commandContext.requireGuild(false);
        if (context != null) { // find game through guild / textchannel
            Game game = this.gameRegistry.get(context.textChannel);
            if (game == null) {
                //private guild?
                for (Game g : this.gameRegistry.getAll().values()) {
                    if (context.guild.getIdLong() == g.getPrivateRoomGuildId()) {
                        game = g;
                        break;
                    }
                }

                if (game == null) {
                    context.getTextChannel().getIdLong();
                    GameSetup setup = this.gameSetupService.channel(context.getTextChannel().getIdLong())
                            .cleanUpInnedPlayers(requireNonNull(context.getJda().getShardManager()));
                    context.reply(this.render.render(setup, context));
                    return true;
                }
            }
            context.reply(game.getStatus().build());
            return true;
        } else {//handle it being issued in a private channel
            //todo handle a player being part of multiple games properly
            boolean issued = false;
            for (Game g : this.gameRegistry.getAll().values()) {
                if (g.isUserPlaying(commandContext.invoker)) {
                    commandContext.reply(g.getStatus().build());
                    issued = true;
                }
            }
            if (!issued) {
                commandContext.replyWithMention(String.format("you aren't playing in any game currently. Say `%s` to get started!",
                        WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER));
                return false;
            }
            return true;
        }
    }
}
