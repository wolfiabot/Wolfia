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

package space.npstr.wolfia.domain.privacy;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.Conversation;
import space.npstr.wolfia.system.EventWaiter;
import space.npstr.wolfia.utils.discord.Emojis;

@Command
public class PrivacyCommand implements BaseCommand, PublicCommand, Conversation {

    public static final String TRIGGER = "privacy";

    private static final String OPTION_READ = "read";
    private static final String OPTION_REQUEST = "request";
    private static final String OPTION_DELETE = "delete";
    private static final String OPTION_DONE = "done";

    private final EventWaiter eventWaiter;
    private final PrivacyService privacyService;

    public PrivacyCommand(EventWaiter eventWaiter, PrivacyService privacyService) {
        this.eventWaiter = eventWaiter;
        this.privacyService = privacyService;
    }

    @Override
    public EventWaiter getEventWaiter() {
        return this.eventWaiter;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public boolean execute(@Nonnull CommandContext context) {
        return start(context);
    }

    @Nonnull
    @Override
    public String help() {
        return "Show Wolfia's Privacy Policy options.";
    }

    @Override
    public boolean start(MessageContext context) {
        return showConversationOptions(context);
    }

    @CheckReturnValue
    private boolean showConversationOptions(MessageContext context) {
        String options = "Welcome to Wolfia privacy policy options! Say "
                + "\n- **" + OPTION_READ + "** to read our privacy policy"
                + "\n- **" + OPTION_REQUEST + "** to request all data this Wolfia instance has collected about you"
                + "\n- **" + OPTION_DELETE + "** to delete all your personal data"
                + "\n- **" + OPTION_DONE + "** when you're done";

        return replyAndWaitForAnswer(context, options, this::optionSelected);
    }

    @CheckReturnValue
    private boolean optionSelected(MessageReceivedEvent event) {
        MessageContext context = new MessageContext(event);
        String rawContent = context.getMessage().getContentRaw();

        if (rawContent.toLowerCase().startsWith(OPTION_READ)) {
            context.reply("Please find the full policy on our website: " + App.PRIVACY_LINK);
            return true;
        }

        if (rawContent.toLowerCase().startsWith(OPTION_REQUEST)) {
            context.reply("You can access your personal data on our privacy policy page: " + App.DATA_ACCESS_LINK);
            return true;
        }

        if (rawContent.toLowerCase().startsWith(OPTION_DELETE)) {
            return new DeleteConversation().start(context);
        }

        if (rawContent.equalsIgnoreCase(OPTION_DONE)) {
            context.reply(Emojis.OK_HAND);
            return true;
        }

        context.replyWithMention("Sorry, I didn't get that.");
        return showConversationOptions(context);
    }

    private class DeleteConversation implements Conversation {

        private static final String OPTION_CONFIRM = "confirm";
        private static final String OPTION_NO = "no";

        @Override
        public EventWaiter getEventWaiter() {
            return eventWaiter;
        }

        @Override
        public boolean start(MessageContext context) {
            return showConversationOptions(context);
        }

        @CheckReturnValue
        private boolean showConversationOptions(MessageContext context) {
            String options = Emojis.WARN + " **ATTENTION, READ CAREFULLY**"
                    + "\nWe understand your request to delete your personal data"
                    + " as a withdrawal of consent to further process your personal data."
                    + " This means your confirmation will have the following effects:"
                    + "\n- Your participation in already recorded games will be anonymized."
                    + "\n- This bot will ignore all your commands."
                    + "\n- You will not be able to play any games with this bot anymore."
                    + "\n- You will be logged out of the dashboard, and will not be able to log in again."
                    + "\n- You will be banned from the Wolfia Lounge."
                    + "\nThese measures are necessary to ensure we comply with your request to not process any of your personal data."
                    + "\n**This action cannot be undone**. Think carefully. Say "
                    + "\n- **" + OPTION_CONFIRM + "** to delete your data with the consequences as described above."
                    + "\n- **" + OPTION_NO + "** to go back.";

            return replyAndWaitForAnswer(context, options, this::optionSelected);
        }

        @CheckReturnValue
        private boolean optionSelected(MessageReceivedEvent event) {
            MessageContext context = new MessageContext(event);
            String rawContent = context.getMessage().getContentRaw();

            if (rawContent.toLowerCase().startsWith(OPTION_CONFIRM)) {
                context.reply("Goodbye.");
                privacyService.dataDelete(event.getAuthor().getIdLong());
                return true;
            }

            if (rawContent.toLowerCase().startsWith(OPTION_NO)) {
                return PrivacyCommand.this.showConversationOptions(context);
            }

            context.replyWithMention("Sorry, I didn't get that.");
            return showConversationOptions(context);
        }
    }
}
