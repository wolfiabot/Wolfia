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

package space.npstr.wolfia.utils.discord;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.springframework.lang.Nullable;
import space.npstr.wolfia.utils.log.LogTheStackException;

/**
 * Everything related to sending RestActions
 * Copy pastad and adjusted from FredBoat where I wrote this for
 */
public class RestActions {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestActions.class);

    //use this to schedule rest actions whenever queueAfter() or similar JDA methods would be used
    // this makes it way easier to track stats + handle failures of such delayed RestActions
    // instead of implementing a ton of overloaded methods in this class
    public static final ScheduledExecutorService restService = Executors.newScheduledThreadPool(10,
            runnable -> new Thread(runnable, "rest-actions-scheduler"));


    //May not be an empty string, as MessageBuilder#build() will throw an exception
    public static MessageCreateData createFrom(String string) {
        return new MessageCreateBuilder().addContent(string).build();
    }

    public static MessageCreateData createFrom(MessageEmbed embed) {
        return new MessageCreateBuilder().setEmbeds(embed).build();
    }

    public static MessageEditData editFrom(String string) {
        return new MessageEditBuilder().setContent(string).build();
    }

    public static MessageEditData editFrom(MessageEmbed embed) {
        return new MessageEditBuilder().setEmbeds(embed).build();
    }


    // ********************************************************************************
    //       Convenience methods that convert input to Message objects and send it
    // ********************************************************************************

    /**
     * @param channel   The channel that should be messaged
     * @param message   Message to be sent
     * @param onSuccess Optional success handler
     * @param onFail    Optional exception handler
     */
    public static void sendMessage(MessageChannel channel, MessageCreateData message,
                                   @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                onFail
        );
    }

    // Message
    public static void sendMessage(MessageChannel channel, MessageCreateData message,
                                   @Nullable Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                null
        );
    }

    // Embed
    public static void sendMessage(MessageChannel channel, MessageEmbed embed,
                                   @Nullable Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                createFrom(embed),
                onSuccess,
                null
        );
    }

    // Embed
    public static void sendMessage(MessageChannel channel, MessageEmbed embed) {
        sendMessage0(
                channel,
                createFrom(embed),
                null,
                null
        );
    }

    // String
    public static void sendMessage(MessageChannel channel, String content,
                                   @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                createFrom(content),
                onSuccess,
                onFail
        );
    }

    // String
    public static void sendMessage(MessageChannel channel, String content,
                                   @Nullable Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                createFrom(content),
                onSuccess,
                null
        );
    }

    // String
    public static void sendMessage(MessageChannel channel, String content) {
        sendMessage0(
                channel,
                createFrom(content),
                null,
                null
        );
    }

    // private
    public static void sendPrivateMessage(User user, String content,
                                          @Nullable Consumer<Message> onSuccess, Consumer<Throwable> onFail) {
        sendPrivateMessage(user, createFrom(content), onSuccess, onFail);
    }

    // private
    // in Wolfia, it is very important that messages reach their destination, that's why private messages require a failure
    // handler, so that each time a private message is coded a conscious decision is made how a failure should be handled
    public static void sendPrivateMessage(User user, MessageCreateData message,
                                          @Nullable Consumer<Message> onSuccess, Consumer<Throwable> onFail) {
        user.openPrivateChannel().queue(
                privateChannel -> sendMessage(privateChannel, message, onSuccess, onFail),
                onFail
        );
    }

    // ********************************************************************************
    //                            Message editing methods
    // ********************************************************************************

    public static void editMessage(Message oldMessage, String newContent) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                editFrom(newContent)
        );
    }

    public static void editMessage(Message oldMessage, MessageEmbed newEmbed) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                editFrom(newEmbed)
        );
    }

    // ********************************************************************************
    //                   Miscellaneous messaging related methods
    // ********************************************************************************

    public static void sendTyping(MessageChannel channel) {
        try {
            channel.sendTyping().queue(
                    null,
                    getJdaRestActionFailureHandler("Could not send typing event in channel " + channel.getId())
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that the message passed in here is actually existing in Discord
    // e.g. dont pass messages in here that were created with a MessageBuilder in our code
    public static void deleteMessage(Message message) {
        try {
            message.delete().queue(
                    null,
                    getJdaRestActionFailureHandler(String.format("Could not delete message %s in channel %s with content%n%s",
                                    message.getId(), message.getChannel().getId(), message.getContentRaw()),
                            ErrorResponse.UNKNOWN_MESSAGE) //user deleted their message, dun care
            );
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(message.getChannel(), e);
        }
    }

    // ********************************************************************************
    //                           Class internal methods
    // ********************************************************************************

    //class internal message sending method
    private static void sendMessage0(MessageChannel channel, MessageCreateData message,
                                     @Nullable Consumer<Message> onSuccess, @Nullable Consumer<Throwable> onFail) {
        Consumer<Message> successWrapper = m -> {
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        Consumer<Throwable> failureWrapper = t -> {
            if (onFail != null) {
                onFail.accept(t);
            } else {
                String info = String.format("Could not sent message%n%s%nwith %s embeds to channel %s in guild %s",
                        message.getContent(), message.getEmbeds().size(), channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "private");
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            channel.sendMessage(message).queue(successWrapper, failureWrapper);
        } catch (InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
            if (e.getPermission() == Permission.MESSAGE_EMBED_LINKS) {
                handleInsufficientPermissionsException(channel, e);
            } else {
                //do not call RestActions#handleInsufficientPermissionsException() from here as that will result in a loop
                log.warn("Could not send message with content {} and {} embeds to channel {} due to missing permission {}",
                        message.getContent(), message.getEmbeds().size(), channel.getIdLong(), e.getPermission().getName(), e);
            }
        }
    }

    //class internal editing method
    private static void editMessage0(MessageChannel channel, long oldMessageId,
                                     MessageEditData newMessage) {

        Consumer<Throwable> failureWrapper = t -> {
            String info = String.format("Could not edit message %s in channel %s in guild %s with new content %s and %s embeds",
                    oldMessageId, channel.getId(),
                    (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null",
                    newMessage.getContent(), newMessage.getEmbeds().size());
            getJdaRestActionFailureHandler(info).accept(t);
        };

        try {
            channel.editMessageById(oldMessageId, newMessage).queue(null, failureWrapper);
        } catch (InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    private static void handleInsufficientPermissionsException(MessageChannel channel,
                                                               InsufficientPermissionException e) {
        //only ever try sending a simple string from here so we don't end up handling a loop of insufficient permissions
        sendMessage(channel, "Please give me the permission to " + " **" + e.getPermission().getName() + "!**");
    }

    //default vanilla version of the rest action failure handler below
    public static Consumer<Throwable> defaultOnFail() {
        return getJdaRestActionFailureHandler("Exception during generic queue()");
    }


    //handles failed JDA rest actions by logging them with an informational string and optionally ignoring some error response codes
    // will print a proper stack trace for exceptions happening in queue(), showing the code leading up to the call of
    // the queue() that failed
    public static Consumer<Throwable> getJdaRestActionFailureHandler(String info, ErrorResponse... ignored) {
        LogTheStackException ex = new LogTheStackException();
        return t -> {
            ex.initCause(t);
            if (t instanceof ErrorResponseException) {
                ErrorResponseException e = (ErrorResponseException) t;
                if (Arrays.asList(ignored).contains(e.getErrorResponse())
                        || e.getErrorCode() == -1 //socket timeout, fuck those
                ) {
                    return;
                }
            }
            log.error("{}\n{}", info, t.getMessage(), ex);
        };
    }

    private RestActions() {}
}
