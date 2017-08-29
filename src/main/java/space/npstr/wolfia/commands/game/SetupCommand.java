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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.entity.SetupEntity;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;

import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Start setting up a game in a channel
 */
public class SetupCommand extends BaseCommand {

    public SetupCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);
    }

    @Override
    public String help() {
        return Config.PREFIX + getMainTrigger() + " [key value]"
                + "\n#Set up games in this channel or show the current setup. Examples:\n"
                + "  " + Config.PREFIX + getMainTrigger() + " game Mafia\n"
                + "  " + Config.PREFIX + getMainTrigger() + " mode Classic\n"
                + "  " + Config.PREFIX + getMainTrigger() + " daylength 3\n"
                + "  " + Config.PREFIX + getMainTrigger();
    }

    @Override
    public boolean execute(final CommandParser.CommandContainer commandInfo) {

        final MessageReceivedEvent event = commandInfo.event;
        final TextChannel channel = event.getTextChannel();
        final Member invoker = event.getMember();
        //will not be null because it will be initialized with default values if there is none
        SetupEntity setup = DbWrapper.getOrCreateEntity(channel.getIdLong(), SetupEntity.class);

        if (commandInfo.args.length == 1) {
            //unsupported input
            commandInfo.reply(formatHelp(invoker.getUser()));
            return false;
        }

        //is this an attempt to edit the setup?
        if (commandInfo.args.length > 1) {
            //is there a game going on?
            if (Games.get(channel.getIdLong()) != null) {
                Wolfia.handleOutputMessage(channel,
                        "%s, there is a game going on in this channel, please wait until it is over to adjust the setup!",
                        invoker.getAsMention());
                return false;
            }

            //is the user allowed to do that?
            if (!invoker.hasPermission(channel, Permission.MESSAGE_MANAGE) && !App.isOwner(invoker)) {
                Wolfia.handleOutputMessage(channel, "%s, you need the following permission to edit the setup of this channel: %s",
                        invoker.getAsMention(), Permission.MESSAGE_MANAGE.getName());
                return false;
            }

            final String option = commandInfo.args[0];
            switch (option.toLowerCase()) {
                case "game":
                    try {
                        setup.setGame(Games.valueOf(commandInfo.args[1].toUpperCase()));
                        setup.setMode(Games.getInfo(setup.getGame()).getDefaultMode());
                        setup = DbWrapper.merge(setup);
                    } catch (final IllegalArgumentException ex) {
                        Wolfia.handleOutputMessage(channel, "%s, no such game is supported by this bot: ", invoker.getAsMention(), commandInfo.args[1]);
                        return false;
                    }
                    break;
                case "mode":
                    try {
                        setup.setMode(GameInfo.GameMode.valueOf(commandInfo.args[1].toUpperCase()));
                        setup = DbWrapper.merge(setup);
                    } catch (final IllegalArgumentException ex) {
                        Wolfia.handleOutputMessage(channel, "%s, no such mode is supported by this game: %s", invoker.getAsMention(), commandInfo.args[1]);
                        return false;
                    }
                    break;
                case "daylength":
                    try {
                        final long minutes = Long.valueOf(commandInfo.args[1]);
                        if (minutes > 10) {
                            Wolfia.handleOutputMessage(channel, "%s, day lengths of more than 10 minutes are not supported currently.", invoker.getAsMention());
                            return false;
                        } else if (minutes < 1) {
                            Wolfia.handleOutputMessage(channel, "%s, day length must be at least one minute.", invoker.getAsMention());
                            return false;
                        }
                        setup.setDayLength(minutes, TimeUnit.MINUTES);
                        setup = DbWrapper.merge(setup);
                    } catch (final NumberFormatException ex) {
                        Wolfia.handleOutputMessage(channel, "%s, use a number to set the day length!", invoker.getAsMention());
                        return false;
                    }
                    break;
                //future ideas:
//                case "nightlength":
//                case "roles":
//                case "playercount":
//                case "handleTIE":
//                    etc
                default:
                    //didn't understand the input
                    commandInfo.reply(formatHelp(invoker.getUser()));
                    return false;
            }
        }
        //show the status quo
        setup.postStatus();
        return true;
    }
}
