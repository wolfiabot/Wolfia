/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain.staff;

import java.util.Optional;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.domain.Conversation;
import space.npstr.wolfia.system.EventWaiter;
import space.npstr.wolfia.utils.discord.Emojis;

@Command
public class StaffCommand implements BaseCommand, Conversation, PublicCommand {

    private static final String OPTION_PROFILE = "profile";
    private static final String OPTION_WOOF = "woof";
    private static final String OPTION_DONE = "done";

    private final StaffService staffService;
    private final EventWaiter eventWaiter;
    private final StaffProfileConversation staffProfileConversation;
    private final StaffRoleConversation staffRoleConversation;

    public StaffCommand(StaffService staffService, EventWaiter eventWaiter) {
        this.staffService = staffService;
        this.eventWaiter = eventWaiter;
        this.staffProfileConversation = new StaffProfileConversation(staffService, eventWaiter);
        this.staffRoleConversation = new StaffRoleConversation(eventWaiter);
    }

    @Override
    public String getTrigger() {
        return "staff";
    }

    @Override
    public String help() {
        return "Do staff things.";
    }

    @Override
    public EventWaiter getEventWaiter() {
        return this.eventWaiter;
    }

    @Override
    public boolean execute(CommandContext context) {
        GuildCommandContext guildCommandContext = context.requireGuild(false);
        if (guildCommandContext == null) {
            return false;
        }
        boolean isStaff = Optional.ofNullable(this.staffService.user(context.getInvoker().getIdLong()).get())
                .map(StaffMember::isActive).orElse(false);
        if (!isStaff && !context.isOwner()) {
            context.replyWithName("I'm sorry, you are not a member of the Wolfia staff, so you cannot invoke this command.");
            return false;
        }

        return this.start(context);
    }

    @Override
    public boolean start(MessageContext context) {
        return showConversationOptions(context);
    }

    private boolean showConversationOptions(MessageContext context) {
        String options = "Welcome to staff commands! What do you want to do? Say "
                + "\n- **" + OPTION_PROFILE + "** to edit your staff profile"
                + "\n- **" + OPTION_WOOF + "** to post a gif of a flying wolf"
                + "\n- **" + OPTION_DONE + "** when you're done";

        return replyAndWaitForAnswer(context, options, this::optionSelected);
    }

    private boolean optionSelected(MessageReceivedEvent event) {
        MessageContext context = new MessageContext(event);
        String rawContent = context.getMessage().getContentRaw();

        if (rawContent.toLowerCase().startsWith(OPTION_PROFILE)) {
            return this.staffProfileConversation.start(context);
        }

        if (rawContent.toLowerCase().startsWith(OPTION_WOOF)) {
            return this.staffRoleConversation.start(context);
        }

        if (rawContent.equalsIgnoreCase(OPTION_DONE)) {
            context.reply(Emojis.OK_HAND);
            return true;
        }

        context.replyWithMention("Sorry, I didn't get that.");
        return showConversationOptions(context);
    }
}
