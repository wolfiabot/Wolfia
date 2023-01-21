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

package space.npstr.wolfia.commands;

import io.prometheus.client.Collector;
import java.awt.Color;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.CheckReturnValue;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import space.npstr.wolfia.system.ApplicationInfoProvider;
import space.npstr.wolfia.system.metrics.MetricsRegistry;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Contexts intended for fast usage, dont save these in any kind of variables
 */
public class MessageContext implements Context {

    // blueish black that reminds of a clear nights sky
    // #001830
    public static final Color BLACKIA = new Color(0, 24, 48);

    //@formatter:off
    @NonNull public final MessageChannel channel;
    @NonNull public final User invoker;
    @NonNull public final Message msg;
    @NonNull public final MessageReceivedEvent event;
    @NonNull public final JDA jda;
    //@formatter:on

    private final ApplicationInfoProvider appInfoProvider;

    public MessageContext(@NonNull final MessageReceivedEvent event) {
        this.channel = event.getChannel();
        this.invoker = event.getAuthor();
        this.msg = event.getMessage();
        this.event = event;
        this.jda = event.getJDA();
        this.appInfoProvider = new ApplicationInfoProvider(event.getJDA().getShardManager());
    }


    @Override
    @NonNull
    @CheckReturnValue
    public MessageChannel getChannel() {
        return this.channel;
    }

    @Override
    @NonNull
    @CheckReturnValue
    public User getInvoker() {
        return this.invoker;
    }

    @Override
    @NonNull
    @CheckReturnValue
    public Message getMessage() {
        return this.msg;
    }

    @Override
    @NonNull
    @CheckReturnValue
    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    @Override
    @NonNull
    @CheckReturnValue
    public JDA getJda() {
        return this.jda;
    }

    @Override
    @CheckReturnValue
    public Optional<Guild> getGuild() {
        return this.event.isFromGuild() ? Optional.of(this.event.getGuild()) : Optional.empty();
    }

    @Override
    @CheckReturnValue
    public Optional<Member> getMember() {
        return Optional.ofNullable(this.event.getMember());
    }

    /**
     * @return true if the invoker is the bot owner, false otherwise
     */
    public boolean isOwner() {
        return this.appInfoProvider.isOwner(this.invoker);
    }


    // ********************************************************************************
    //                         Convenience reply methods
    // ********************************************************************************
    // NOTE: they all try to end up in the reply0 method for consistent behaviour


    public void reply(@NonNull final MessageEmbed embed) {
        reply0(RestActions.from(embed), null);
    }

    public void reply(@NonNull final EmbedBuilder eb) {
        reply(eb.build());
    }

    public void reply(@NonNull final Message message, @Nullable final Consumer<Message> onSuccess) {
        reply0(message, onSuccess);
    }

    public void reply(@NonNull final Message message) {
        reply0(message, null);
    }

    public void reply(@NonNull final String message) {
        reply(new MessageBuilder().append(message).build(), null);
    }

    public void replyWithName(@NonNull final String message, @Nullable final Consumer<Message> onSuccess) {
        final Optional<Member> member = getMember();
        if (member.isPresent()) {
            reply(TextchatUtils.prefaceWithName(member.get(), message, true), onSuccess);
        } else {
            reply(TextchatUtils.prefaceWithName(invoker, message, true), onSuccess);
        }
    }

    public void replyWithName(@NonNull final String message) {
        replyWithName(message, null);
    }

    public void replyWithMention(@NonNull final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(TextchatUtils.prefaceWithMention(invoker, message), onSuccess);
    }

    public void replyWithMention(@NonNull final String message) {
        replyWithMention(message, null);
    }


    public void replyPrivate(@NonNull final String message, @Nullable final Consumer<Message> onSuccess, @NonNull final Consumer<Throwable> onFail) {
        RestActions.sendPrivateMessage(invoker, message, onSuccess, onFail);
    }

    public void sendTyping() {
        RestActions.sendTyping(channel);
    }

    /**
     * @return a general purpose preformatted builder for embeds
     */
    @NonNull
    public static EmbedBuilder getDefaultEmbedBuilder() {
        return new EmbedBuilder()
                .setColor(BLACKIA);
    }


    // ********************************************************************************
    //                         Internal context stuff
    // ********************************************************************************

    private void reply0(@NonNull final Message message, @Nullable final Consumer<Message> onSuccess) {
        long started = System.nanoTime();

        Consumer<Message> successWrapper = m -> {
            MetricsRegistry.commandResponseTime.observe((System.nanoTime() - started) / Collector.NANOSECONDS_PER_SECOND);
            long in = getMessage().getTimeCreated().toInstant().toEpochMilli();
            long out = m.getTimeCreated().toInstant().toEpochMilli();
            MetricsRegistry.commandTotalTime.observe((out - in) / Collector.MILLISECONDS_PER_SECOND);
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };

        RestActions.sendMessage(channel, message, successWrapper);
    }
}
