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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ICommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.utils.App;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Start setting up a game in a channel
 */
public class SetupCommand implements ICommand {

    public static final String COMMAND = "setup";

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {

        final MessageReceivedEvent e = commandInfo.event;
        //will not be null because it will be initialized with default values if there is none
        SetupEntity setup = DbWrapper.getEntity(channel.getIdLong(), SetupEntity.class);

        //is this an attempt to edit the setup?
        if (commandInfo.args.length > 1) {
            //is there a game going on?
            if (space.npstr.wolfia.game.Games.get(e.getTextChannel().getIdLong()) != null) {
                Wolfia.handleOutputMessage(e.getTextChannel(),
                        "%s, there is a game going on in this channel, please wait until it is over to adjust the setup!",
                        TextchatUtils.userAsMention(e.getAuthor().getIdLong()));
                return;
            }

            //is the user allowed to do that?
            if (!e.getMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE) && !App.isOwner(e.getAuthor())) {
                Wolfia.handleOutputMessage(e.getTextChannel(), "%s, you need the following permission to edit the setup of this channel: %s",
                        e.getAuthor().getAsMention(), Permission.MESSAGE_MANAGE.getName());
                return;
            }

            final String option = commandInfo.args[0];
            switch (option.toLowerCase()) {
                case "game":
                    try {
                        setup.setGame(Games.valueOf(commandInfo.args[1]));
                        setup = DbWrapper.merge(setup);
                    } catch (final IllegalArgumentException ex) {
                        Wolfia.handleOutputMessage(e.getTextChannel(), "%s, no such game is supported by this bot.", e.getAuthor().getAsMention());
                        return;
                    }
                    break;
                case "mode":
                    try {
                        setup.setMode(commandInfo.args[1].toUpperCase());
                        setup = DbWrapper.merge(setup);
                    } catch (final IllegalArgumentException ex) {
                        Wolfia.handleOutputMessage(e.getTextChannel(), "%s, no such mode is supported by this game.", e.getAuthor().getAsMention());
                        return;
                    }
                    break;
                case "daylength":
                    try {
                        final long minutes = Long.valueOf(commandInfo.args[1]);
                        if (minutes > 10) {
                            Wolfia.handleOutputMessage(e.getTextChannel(), "%s, day lengths of more than 10 minutes are not supported currently.", e.getAuthor().getAsMention());
                            return;
                        } else if (minutes < 1) {
                            Wolfia.handleOutputMessage(e.getTextChannel(), "%s, day length must be at least one minute.", e.getAuthor().getAsMention());
                            return;
                        }
                        setup.setDayLength(minutes, TimeUnit.MINUTES);
                        setup = DbWrapper.merge(setup);
                    } catch (final NumberFormatException ex) {
                        Wolfia.handleOutputMessage(e.getTextChannel(), "%s, use a number to set the day length!", e.getAuthor().getAsMention());
                        return;
                    }
                    break;
                //future ideas:
//                case "nightlength":
//                case "roles":
//                case "playercount":
//                case "handleTIE":
//                    etc
                default:
                    //didn't understand the input, will show the status quo
                    break;
            }
        }
        //show the status quo
        setup.postStats();
    }

    @Override
    public String help() {
        return "```usage: " + Config.PREFIX + COMMAND + "\nto start setting up a game in this channel```";
    }
}
