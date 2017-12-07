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
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.db.entities.SetupEntity;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
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

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [key value]"
                + "\n#Set up games in this channel or show the current setup. Examples:\n"
                + "  " + invocation() + " game Mafia\n"
                + "  " + invocation() + " mode Classic\n"
                + "  " + invocation() + " daylength 3\n"
                + "  " + invocation();
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) throws DatabaseException {

        if (context.channel.getType() != ChannelType.TEXT) {
            context.reply("Setup are for guild channels only");
            return false;
        }
        final TextChannel channel = (TextChannel) context.channel;
        SetupEntity setup = SetupEntity.load(channel.getIdLong());

        if (context.args.length == 1) {
            //unsupported input
            context.help();
            return false;
        }

        //is this an attempt to edit the setup?
        if (context.args.length > 1) {
            //is there a game going on?
            if (Games.get(channel.getIdLong()) != null) {
                context.replyWithMention("there is a game going on in this channel, please wait until it is over to adjust the setup!");
                return false;
            }

            //is the user allowed to do that?
            final Member member = context.getMember();
            if (member == null || (!member.hasPermission(channel, Permission.MESSAGE_MANAGE) && !context.isOwner())) {
                context.replyWithMention("you need the following permission to edit the setup of this channel: "
                        + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
                return false;
            }

            final String option = context.args[0];
            switch (option.toLowerCase()) {
                case "game":
                    try {
                        setup = setup.setGame(Games.valueOf(context.args[1].toUpperCase()))
                                .setMode(Games.getInfo(setup.getGame()).getDefaultMode())
                                .save();
                    } catch (final IllegalArgumentException ex) {
                        context.replyWithMention("no such game is supported by this bot: " + TextchatUtils.defuseMentions(context.args[1]));
                        return false;
                    }
                    break;
                case "mode":
                    try {
                        setup = setup.setMode(GameInfo.GameMode.valueOf(context.args[1].toUpperCase()))
                                .save();
                    } catch (final IllegalArgumentException ex) {
                        context.replyWithMention("no such mode is supported by this game: " + TextchatUtils.defuseMentions(context.args[1]));
                        return false;
                    }
                    break;
                case "daylength":
                    try {
                        final long minutes = Long.parseLong(context.args[1]);
                        if (minutes > 10) {
                            context.replyWithMention("day lengths of more than 10 minutes are not supported currently.");
                            return false;
                        } else if (minutes < 1) {
                            context.replyWithMention("day length must be at least one minute.");
                            return false;
                        }
                        setup = setup.setDayLength(minutes, TimeUnit.MINUTES)
                                .save();
                    } catch (final NumberFormatException ex) {
                        context.replyWithMention("use a number to set the day length!");
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
                    context.help();
                    return false;
            }
        }
        //show the status quo
        context.reply(setup.getStatus());
        return true;
    }
}
