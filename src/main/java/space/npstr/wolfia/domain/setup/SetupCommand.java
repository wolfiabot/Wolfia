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

package space.npstr.wolfia.domain.setup;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.game.GameRegistry;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static java.util.Objects.requireNonNull;

/**
 * Created by napster on 12.05.17.
 * <p>
 * Start setting up a game in a channel
 */
@Command
public class SetupCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "setup";

    private final GameSetupService gameSetupService;
    private final GameSetupRender render;
    private final GameRegistry gameRegistry;

    public SetupCommand(GameSetupService service, GameSetupRender render, GameRegistry gameRegistry) {
        this.gameSetupService = service;
        this.render = render;
        this.gameRegistry = gameRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
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
    public boolean execute(@Nonnull final CommandContext commandContext) {
        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        long channelId = context.textChannel.getIdLong();
        GameSetupService.Action setupAction = this.gameSetupService.channel(channelId);
        final AtomicBoolean blewUp = new AtomicBoolean(false);

        if (context.args.length >= 1 && "reset".equalsIgnoreCase(context.args[0])) {
            if (allowedToEditSetup(context)) {
                setupAction.reset();
                context.replyWithMention("game setup of this channel has been reset.");
                GameSetup setup = setupAction.cleanUpInnedPlayers(requireNonNull(context.getJda().getShardManager()));
                context.reply(this.render.render(setup, context));
                return true;
            } else {
                context.replyWithMention("you need the following permission to reset the setup of this channel: "
                        + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
                return false;
            }
        }

        if (context.args.length == 1) {
            //unsupported input
            context.help();
            return false;
        }

        //is this an attempt to edit the setup?
        if (context.args.length > 1) {
            //is there a game going on?
            if (this.gameRegistry.get(context.textChannel) != null) {
                context.replyWithMention("there is a game going on in this channel, please wait until it is over to adjust the setup!");
                return false;
            }

            //is the user allowed to do that?
            if (!allowedToEditSetup(context)) {
                context.replyWithMention("you need the following permission to edit the setup of this channel: "
                        + "**" + Permission.MESSAGE_MANAGE.getName() + "**");
                return false;
            }

            final String option = context.args[0];
            switch (option.toLowerCase()) {
                case "game":
                    try {
                        Games game = Games.valueOf(context.args[1].toUpperCase());
                        setupAction.setGame(game);
                    } catch (final IllegalArgumentException ex) {
                        context.replyWithMention("no such game is supported by this bot: " + TextchatUtils.defuseMentions(context.args[1]));
                        blewUp.set(true);
                    }
                    break;
                case "mode":
                    try {
                        GameInfo.GameMode mode = GameInfo.GameMode.valueOf(context.args[1].toUpperCase());
                        GameInfo gameInfo = Games.getInfo(setupAction.getOrDefault().getGame());
                        if (gameInfo.getSupportedModes().contains(mode)) {
                            setupAction.setMode(mode);
                        } else {
                            context.replyWithMention("no such mode is supported by this game: " + TextchatUtils.defuseMentions(context.args[1]));
                        }
                    } catch (final IllegalArgumentException ex) {
                        context.replyWithMention("no such mode is supported by this game: " + TextchatUtils.defuseMentions(context.args[1]));
                        blewUp.set(true);
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
                        setupAction.setDayLength(Duration.ofMinutes(minutes));
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
        if (blewUp.get()) {
            return false;//feedback has been given
        }
        //show the status quo
        GameSetup setup = setupAction.cleanUpInnedPlayers(requireNonNull(context.getJda().getShardManager()));
        context.reply(this.render.render(setup, context));
        return true;
    }

    private boolean allowedToEditSetup(GuildCommandContext context) {
        Optional<Member> member = context.getMember();
        if (member.isEmpty()) {
            return false;
        }
        return member.get().hasPermission(context.getTextChannel(), Permission.MESSAGE_MANAGE)
                || context.isOwner();
    }
}
