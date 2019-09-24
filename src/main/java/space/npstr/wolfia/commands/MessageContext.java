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
import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Created by napster on 16.11.17.
 * <p>
 * Contexts intended for fast usage, dont save these in any kind of variables
 */
public class MessageContext implements Context {

    public static final Color BLACKIA = new Color(0, 24, 48); //blueish black that reminds of a clear nights sky

    //@formatter:off
    @Nonnull public final MessageChannel channel;
    @Nonnull public final User invoker;
    @Nonnull public final Message msg;
    @Nonnull public final MessageReceivedEvent event;
    @Nonnull public final JDA jda;
    //@formatter:on


    public MessageContext(@Nonnull final MessageReceivedEvent event) {
        this.channel = event.getChannel();
        this.invoker = event.getAuthor();
        this.msg = event.getMessage();
        this.event = event;
        this.jda = event.getJDA();
    }


    @Override
    @Nonnull
    @CheckReturnValue
    public MessageChannel getChannel() {
        return this.channel;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public User getInvoker() {
        return this.invoker;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public Message getMessage() {
        return this.msg;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    @Override
    @Nonnull
    @CheckReturnValue
    public JDA getJda() {
        return this.jda;
    }

    @Override
    @Nullable
    @CheckReturnValue
    public Guild getGuild() {
        return this.event.getGuild();
    }

    @Override
    @Nullable
    @CheckReturnValue
    public Member getMember() {
        return this.event.getMember();
    }

    @Nonnull
    @CheckReturnValue
    public String getMyOwnNick() {
        final Guild g = getGuild();
        if (g != null) {
            return g.getSelfMember().getEffectiveName();
        } else {
            return this.jda.getSelfUser().getName();
        }
    }

    //name or nickname of the invoker issuing the command
    @Nonnull
    @CheckReturnValue
    public String getEffectiveName() {
        if (this.channel instanceof TextChannel) {
            final Member member = ((TextChannel) this.channel).getGuild().getMember(this.invoker);
            if (member != null) {
                return member.getEffectiveName();
            }
        }
        return this.invoker.getName();
    }

    @Nonnull
    @CheckReturnValue
    //nickname of the member entity of the provided user id in this guild or their user name or a default value
    public String getEffectiveName(final long userId) {
        final Guild g = getGuild();
        if (g != null) {
            final Member member = g.getMemberById(userId);
            if (member != null) {
                return member.getEffectiveName();
            }
        }
        //fallback to global user lookup
        final User u = Wolfia.getUserById(userId);
        if (u != null) {
            return u.getName();
        } else {
            return DiscordUser.UNKNOWN_NAME; //todo db lookup
        }
    }

    /**
     * @return true if the invoker is the bot owner, false otherwise
     */
    public boolean isOwner() {
        return App.isOwner(this.invoker);
    }


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
            reply(TextchatUtils.prefaceWithName(invoker, message, true), onSuccess);
        }
    }

    public void replyWithName(@Nonnull final String message) {
        replyWithName(message, null);
    }

    public void replyWithMention(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(TextchatUtils.prefaceWithMention(invoker, message), onSuccess);
    }

    public void replyWithMention(@Nonnull final String message) {
        replyWithMention(message, null);
    }


    public void replyPrivate(@Nonnull final String message, @Nullable final Consumer<Message> onSuccess, @Nonnull final Consumer<Throwable> onFail) {
        RestActions.sendPrivateMessage(invoker, message, onSuccess, onFail);
    }

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
        RestActions.sendTyping(channel);
    }


    //checks whether we have the provided permissions for the provided channel
    @CheckReturnValue
    public static boolean hasPermissions(@Nonnull final TextChannel tc, final Permission... permissions) {
        return tc.getGuild().getSelfMember().hasPermission(tc, permissions);
    }

    /**
     * @return a general purpose preformatted builder for embeds
     */
    @Nonnull
    public static EmbedBuilder getDefaultEmbedBuilder() {
        return RestActions.getEmbedBuilder()
                .setColor(BLACKIA);
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
        RestActions.sendMessage(channel, message, onSuccess);
    }
}
