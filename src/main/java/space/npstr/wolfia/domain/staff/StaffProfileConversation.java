/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckReturnValue;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.domain.Conversation;
import space.npstr.wolfia.system.EventWaiter;
import space.npstr.wolfia.utils.discord.Emojis;

/**
 * Conversation command execution for staffers to manage their staff profiles
 */
class StaffProfileConversation implements Conversation {

    private static final String OPTION_ENABLE = "enable";
    private static final String OPTION_DISABLE = "disable";
    private static final String OPTION_SET_SLOGAN = "set slogan";
    private static final String OPTION_REMOVE_SLOGAN = "remove slogan";
    private static final String OPTION_SET_LINK = "set link";
    private static final String OPTION_REMOVE_LINK = "remove link";
    private static final String OPTION_DONE = "done";

    private static final URI TEAM_WEBSITE = URI.create("https://bot.wolfia.party/team");
    private static final int MAX_SLOGAN_LENGTH = 100;

    private final StaffService staffService;
    private final EventWaiter eventWaiter;

    StaffProfileConversation(StaffService staffService, EventWaiter eventWaiter) {
        this.staffService = staffService;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public EventWaiter getEventWaiter() {
        return this.eventWaiter;
    }

    @Override
    public String getTimeoutMessage() {
        return "Canceled your staff profile editing.";
    }

    @Override
    public boolean start(MessageContext context) {
        Optional<StaffMember> staffMember = this.staffService.user(context.getInvoker().getIdLong()).get();
        if (staffMember.isEmpty()) {
            context.replyWithName("I'm sorry, you are not a member of the Wolfia staff, so you cannot invoke this command.");
            return false;
        }

        List<User> mentions = context.getMessage().getMentionedUsers();
        if (!mentions.isEmpty()) {
            if (!context.isOwner()) {
                context.replyWithName("You cannot edit the staff profile of another staff member if you are not the owner.");
                return false;
            }

            User target = mentions.get(0);
            staffMember = this.staffService.user(target.getIdLong()).get();
            if (staffMember.isEmpty()) {
                context.replyWithName("I'm sorry, " + target.getAsMention() + " is not a member of the Wolfia staff, they have no staff profile.");
                return false;
            }
        }
        return showStaffProfileAndOptions(context, staffMember.get(), "");
    }

    @CheckReturnValue
    private boolean showStaffProfileAndOptions(MessageContext context, StaffMember staffMember, String plainMessage) {
        String options = "How do you want to edit your staff profile? Say "
                + "\n- **" + OPTION_ENABLE + "** to enable your staff profile"
                + "\n- **" + OPTION_DISABLE + "** to disable your staff profile"
                + "\n- **" + OPTION_SET_SLOGAN + "** to set a slogan for your staff profile"
                + "\n- **" + OPTION_REMOVE_SLOGAN + "** to remove the slogan from your staff profile"
                + "\n- **" + OPTION_SET_LINK + "** to set a link for your staff profile"
                + "\n- **" + OPTION_REMOVE_LINK + "** to remove the link from your staff profile"
                + "\n- **" + OPTION_DONE + "** if you're done editing your staff profile";

        return show(context, staffMember, plainMessage)
                && replyAndWaitForAnswer(context, options, e -> optionSelected(e, staffMember));
    }

    //plainMessage can be empty
    @CheckReturnValue
    private boolean show(MessageContext context, StaffMember staffMember, String plainMessage) {
        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Staff Profile")
                .addField("Name", staffMember.getName() + "#" + staffMember.getDiscriminator(), true)
                .addField("Staff Function", staffMember.getFunction().name(), true)
                .addField("Enabled", staffMember.isEnabled() ? Emojis.CHECK : Emojis.X, true)
                .addField("Slogan", staffMember.getSlogan().orElse(""), true)
                .addField("Link", staffMember.getLink().map(URI::toString).orElse(""), true);


        Message message = new MessageBuilder(plainMessage)
                .setEmbed(embedBuilder.build())
                .build();

        context.reply(message);
        return true;
    }

    @CheckReturnValue
    private boolean optionSelected(MessageReceivedEvent event, StaffMember staffMember) {
        MessageContext context = new MessageContext(event);
        String rawContent = context.getMessage().getContentRaw();

        if (rawContent.toLowerCase().startsWith(OPTION_ENABLE)) {
            StaffMember updated = this.staffService.user(staffMember.getDiscordId()).enable();
            return showStaffProfileAndOptions(context, updated, "Enabled your staff profile! Check " + TEAM_WEBSITE.toString());
        }

        if (rawContent.toLowerCase().startsWith(OPTION_DISABLE)) {
            StaffMember updated = this.staffService.user(staffMember.getDiscordId()).disable();
            return showStaffProfileAndOptions(context, updated, "Disabled your staff profile. " + Emojis.RIP);
        }

        if (rawContent.toLowerCase().startsWith(OPTION_SET_SLOGAN)) {
            String args = rawContent.substring(OPTION_SET_SLOGAN.length()).trim();
            if (!args.isEmpty()) {
                return setSlogan(context, args, staffMember);
            }
            return replyAndWaitForAnswer(context, "Which slogan do you want to set?", e -> setSlogan(e, staffMember));
        }

        if (rawContent.toLowerCase().startsWith(OPTION_REMOVE_SLOGAN)) {
            StaffMember updated = this.staffService.user(staffMember.getDiscordId()).removeSlogan();
            return showStaffProfileAndOptions(context, updated, "Removed your slogan.");
        }

        if (rawContent.toLowerCase().startsWith(OPTION_SET_LINK)) {
            String args = rawContent.substring(OPTION_SET_LINK.length()).trim();
            if (!args.isEmpty()) {
                return setLink(context, args, staffMember);
            }
            return replyAndWaitForAnswer(context, "Which link do you want to set?", e -> setLink(e, staffMember));
        }

        if (rawContent.toLowerCase().startsWith(OPTION_REMOVE_LINK)) {
            StaffMember updated = this.staffService.user(staffMember.getDiscordId()).removeLink();
            return showStaffProfileAndOptions(context, updated, "Removed your link.");
        }

        if (rawContent.equalsIgnoreCase(OPTION_DONE)) {
            context.reply(Emojis.OK_HAND);
            return true;
        }

        return showStaffProfileAndOptions(context, staffMember, "Sorry, I didn't get that.");
    }

    @CheckReturnValue
    private boolean setSlogan(MessageReceivedEvent event, StaffMember staffMember) {
        MessageContext context = new MessageContext(event);
        String slogan = context.getMessage().getContentRaw();

        return setSlogan(context, slogan, staffMember);
    }

    @CheckReturnValue
    private boolean setSlogan(MessageContext context, String slogan, StaffMember staffMember) {
        if (slogan.length() > MAX_SLOGAN_LENGTH) {
            return showStaffProfileAndOptions(context, staffMember, Emojis.X + ": Please keep your slogan to a maximum length of " + MAX_SLOGAN_LENGTH + ".");
        }
        StaffMember updated = this.staffService.user(staffMember.getDiscordId())
                .setSlogan(slogan);
        return showStaffProfileAndOptions(context, updated, "Set your slogan:");
    }

    @CheckReturnValue
    private boolean setLink(MessageReceivedEvent event, StaffMember staffMember) {
        MessageContext context = new MessageContext(event);
        String link = context.getMessage().getContentRaw();

        return setLink(context, link, staffMember);
    }

    @CheckReturnValue
    private boolean setLink(MessageContext context, String link, StaffMember staffMember) {
        try {
            URI uri = new URL(link).toURI();
            StaffMember updated = this.staffService.user(staffMember.getDiscordId())
                    .setLink(uri);
            return showStaffProfileAndOptions(context, updated, "Set your link:");
        } catch (MalformedURLException | URISyntaxException e) {
            return showStaffProfileAndOptions(context, staffMember, Emojis.X + ": Failed to parsed your link. Please double check it's a real link.");
        }

    }
}
