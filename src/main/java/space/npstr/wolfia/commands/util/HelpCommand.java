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

package space.npstr.wolfia.commands.util;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.commands.game.StartCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.setup.InCommand;
import space.npstr.wolfia.utils.discord.TextchatUtils;

public class HelpCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "help";

    private final CommRegistry commRegistry;

    public HelpCommand(final CommRegistry commRegistry) {
        this.commRegistry = commRegistry;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " [command]"
                + "\n#Send you Wolfia's general help and links to documentation, or see the help for a specific command. Examples:"
                + "\n  " + invocation()
                + "\n  " + invocation() + " shoot";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {
        if (Launcher.getBotContext().getWolfiaConfig().isDebug() && !context.isOwner()) {
            return true;//dont answer the help command in debug mode unless it's the owner
        }

        final MessageChannel channel = context.channel;
        if (context.hasArguments() && channel.getType() == ChannelType.TEXT && ((TextChannel) channel).canTalk()) {
            final BaseCommand command = this.commRegistry.getCommand(context.args[0]);
            final String answer;
            if (!(command instanceof PublicCommand)) {
                answer = String.format("There is no command registered for `%s`. Use `%s` to see all available commands!",
                        TextchatUtils.defuseMentions(context.args[0]), WolfiaConfig.DEFAULT_PREFIX + CommandsCommand.TRIGGER);
            } else {
                answer = TextchatUtils.asMarkdown(command.getHelp());
            }
            context.reply(answer);
            return true;
        }

        final String help = String.format("Hi %s,%nyou can find %s's **documentation** and a **full list of commands** under%n<%s>"
                        + "%n%n**To invite the bot to your server please follow this link**:%n<%s>"
                        + "%n%nDrop by the Wolfia Lounge to play games, get support, leave feedback, get notified of updates and vote on the roadmap:%n<%s>"
                        + "%n%nCode open sourced on Github:%n<%s>"
                        + "%n%nCreated and hosted by Napster:%n<%s>",
                context.invoker.getName(), context.invoker.getJDA().getSelfUser().getName(), App.DOCS_LINK, App.INVITE_LINK,
                App.WOLFIA_LOUNGE_INVITE, App.GITHUB_LINK, "https://npstr.space");

        final Consumer<Message> onSuccess = m -> {
            if (channel.getType() == ChannelType.TEXT && !((TextChannel) channel).canTalk()) {
                return;
            }
            final String answer = String.format("sent you a PM with the help!"
                            + "\nUse `%s` and `%s` to start games."
                            + "\nSay `%s` to show all commands."
                            + "\nSay `%s [command]` to show help for a specific command.",
                    WolfiaConfig.DEFAULT_PREFIX + InCommand.TRIGGER, WolfiaConfig.DEFAULT_PREFIX + StartCommand.TRIGGER,
                    WolfiaConfig.DEFAULT_PREFIX + CommandsCommand.TRIGGER,
                    WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER);
            context.replyWithMention(answer);
        };
        final Consumer<Throwable> onFail = t -> {
            if (channel.getType() == ChannelType.TEXT && ((TextChannel) channel).canTalk())
                context.replyWithMention("can't send you a private message with the help."
                        + " Please unblock me or change your privacy settings.");
        };

        context.replyPrivate(help, onSuccess, onFail);
        return true;
    }
}
