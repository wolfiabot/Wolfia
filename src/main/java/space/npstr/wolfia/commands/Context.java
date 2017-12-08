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

package space.npstr.wolfia.commands;


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods
 */
@SuppressWarnings("unused")
public abstract class Context {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    @Nullable
    @CheckReturnValue
    public abstract MessageChannel getChannel();

    @Nullable
    @CheckReturnValue
    public abstract User getInvoker();

    //message that triggered this context
    @Nullable
    @CheckReturnValue
    public abstract Message getMessage();

    @Nullable
    @CheckReturnValue
    public abstract MessageReceivedEvent getEvent();

    @Nullable
    @CheckReturnValue
    public abstract JDA getJda();

    @Nullable
    @CheckReturnValue
    public abstract Guild getGuild();

    /**
     * @return Member entity of the invoker
     */
    @Nullable
    @CheckReturnValue
    public abstract Member getMember();


    // ********************************************************************************
    //                         Convenience reply methods
    // ********************************************************************************
    // NOTE: they all try to end up in the reply0 method for consistent behaviour


    public void reply(@Nonnull final MessageEmbed embed) {
        reply0(RestActions.from(embed), null);
    }

    public void reply(@Nonnull final EmbedBuilder eb) {
        reply(eb.build());
    }

    public void reply(@Nonnull final Message message, @Nullable final Consumer<Message> onSuccess) {
        reply0(message, onSuccess);
    }

    public void reply(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(RestActions.getMessageBuilder().append(message).build(), onSuccess);
    }

    public void reply(@Nonnull final Message message) {
        reply(message, null);
    }

    public void reply(@Nonnull final String message) {
        reply(RestActions.getMessageBuilder().append(message).build(), null);
    }

    public void replyWithName(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess) {
        final Member member = getMember();
        if (member != null) {
            reply(TextchatUtils.prefaceWithName(member, message, true), onSuccess);
        } else {
            final User user = getInvoker();
            if (user != null) {
                reply(TextchatUtils.prefaceWithName(user, message, true), onSuccess);
            }
        }
    }

    public void replyWithName(@Nonnull final String message) {
        replyWithName(message, null);
    }

    public void replyWithMention(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess) {
        final User user = getInvoker();
        if (user != null) {
            reply(TextchatUtils.prefaceWithMention(user, message), onSuccess);
        }
    }

    public void replyWithMention(@Nonnull final String message) {
        replyWithMention(message, null);
    }


    public void replyPrivate(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess, @Nonnull final Consumer<Throwable> onFail) {
        final User user = getInvoker();
        if (user != null) {
            RestActions.sendPrivateMessage(user, message, onSuccess, onFail);
        }
    }

//    public void replyFile(@Nonnull File file, @Nullable Message message) {
//        return RestActions.sendFile(getTextChannel(), file, message);
//    }

    public void replyImage(@Nonnull final String url) {
        replyImage(url, null);
    }

    public void replyImage(@Nonnull final String url, @Nullable final String message) {
        reply(RestActions.getMessageBuilder()
                .setEmbed(embedImage(url))
                .append(message != null ? message : "")
                .build()
        );
    }

    public void sendTyping() {
        final MessageChannel channel = getChannel();
        if (channel != null) {
            RestActions.sendTyping(channel);
        }
    }


    //checks whether we have the provided permissions for the provided channel
    @CheckReturnValue
    public static boolean hasPermissions(@Nonnull final TextChannel tc, final Permission... permissions) {
        return tc.getGuild().getSelfMember().hasPermission(tc, permissions);
    }

    public static Color BLACKIA = new Color(0, 24, 48); //blueish black that reminds of a clear nights sky

    /**
     * @return a general purpose preformatted builder for embeds
     */
    @Nonnull
    public static EmbedBuilder getDefaultEmbedBuilder() {
//        User self = channel.getJDA().getSelfUser();
        return RestActions.getEmbedBuilder()
//                .setFooter(self.getName(), self.getEffectiveAvatarUrl())
                .setColor(BLACKIA)
//                .setThumbnail(self.getEffectiveAvatarUrl())
//                .setTimestamp(context.event.getMessage().getCreationTime())
//                .setAuthor(self.getName(), Main.AKI_BOT_INVITE, self.getEffectiveAvatarUrl())
                ;
    }


    // ********************************************************************************
    //                         Internal context stuff
    // ********************************************************************************

    private static MessageEmbed embedImage(final String url) {
        return getDefaultEmbedBuilder()
                .setImage(url)
                .build();
    }

    private void reply0(@Nonnull final Message message, @Nullable final Consumer<Message> onSuccess) {
        final MessageChannel channel = getChannel();
        if (channel == null) {
            return;//todo really?
        }
        RestActions.sendMessage(channel, message, onSuccess);
    }
}
