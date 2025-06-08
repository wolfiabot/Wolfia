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

package space.npstr.wolfia.commands;

import io.micrometer.core.instrument.Timer;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.lang.Nullable;
import space.npstr.wolfia.system.ApplicationInfoProvider;
import space.npstr.wolfia.system.metrics.MetricsService;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Contexts intended for fast usage, dont save these in any kind of variables
 */
public class MessageContext implements Context {

    // blueish black that reminds of a clear nights sky
    // #001830
    public static final Color BLACKIA = new Color(0, 24, 48);

    public final MessageChannel channel;
    public final User invoker;
    public final Message msg;
    public final MessageReceivedEvent event;
    public final JDA jda;
    private final MetricsService metricsService;

    private final ApplicationInfoProvider appInfoProvider;

    public MessageContext(MessageReceivedEvent event, MetricsService metricsService) {
        this.channel = event.getChannel();
        this.invoker = event.getAuthor();
        this.msg = event.getMessage();
        this.event = event;
        this.jda = event.getJDA();
        this.appInfoProvider = new ApplicationInfoProvider(event.getJDA().getShardManager());
        this.metricsService = metricsService;
    }


    @Override
    public MessageChannel getChannel() {
        return this.channel;
    }

    @Override
    public User getInvoker() {
        return this.invoker;
    }

    @Override
    public Message getMessage() {
        return this.msg;
    }

    @Override
    public MessageReceivedEvent getEvent() {
        return this.event;
    }

    @Override
    public JDA getJda() {
        return this.jda;
    }

    @Override
    public Optional<Guild> getGuild() {
        return this.event.isFromGuild() ? Optional.of(this.event.getGuild()) : Optional.empty();
    }

    @Override
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

    public void reply(List<MessageEmbed> embeds) {
        for (MessageEmbed embed : embeds) {
            reply(embed);
        }
    }

    public void reply(MessageEmbed embed) {
        reply0(RestActions.createFrom(embed), null);
    }

    public void reply(EmbedBuilder eb) {
        reply(eb.build());
    }

    public void reply(MessageCreateData message, @Nullable Consumer<Message> onSuccess) {
        reply0(message, onSuccess);
    }

    public void reply(MessageCreateData message) {
        reply0(message, null);
    }

    public void reply(String message) {
        reply(new MessageCreateBuilder().addContent(message).build(), null);
    }

    public void replyWithName(String message, @Nullable Consumer<Message> onSuccess) {
        Optional<Member> member = getMember();
        if (member.isPresent()) {
            reply(TextchatUtils.prefaceWithName(member.get(), message, true), onSuccess);
        } else {
            reply(TextchatUtils.prefaceWithName(invoker, message, true), onSuccess);
        }
    }

    public void replyWithName(String message) {
        replyWithName(message, null);
    }

    public void replyWithMention(String message, @Nullable Consumer<Message> onSuccess) {
        reply(TextchatUtils.prefaceWithMention(invoker, message), onSuccess);
    }

    public void replyWithMention(String message) {
        replyWithMention(message, null);
    }


    public void replyPrivate(String message, @Nullable Consumer<Message> onSuccess, Consumer<Throwable> onFail) {
        RestActions.sendPrivateMessage(invoker, message, onSuccess, onFail);
    }

    public void sendTyping() {
        RestActions.sendTyping(channel);
    }

    /**
     * @return a general purpose preformatted builder for embeds
     */
    public static EmbedBuilder getDefaultEmbedBuilder() {
        return new EmbedBuilder()
                .setColor(BLACKIA);
    }


    // ********************************************************************************
    //                         Internal context stuff
    // ********************************************************************************

    private void reply0(MessageCreateData message, @Nullable Consumer<Message> onSuccess) {
        Timer.Sample sample = Timer.start();

        Consumer<Message> successWrapper = m -> {
            sample.stop(metricsService.commandResponseTime());
            Instant in = getMessage().getTimeCreated().toInstant();
            Instant out = m.getTimeCreated().toInstant();
            Duration between = Duration.between(in, out);
            if (between.isNegative()) {
                //it has happened before...wtf
                between = Duration.ZERO;
            }
            metricsService.commandTotalTime().record(between);
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };

        RestActions.sendMessage(channel, message, successWrapper);
    }
}
