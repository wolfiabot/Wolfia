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

package space.npstr.wolfia.utils.discord;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
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
    @NonNull
    public static Message from(final String string) {
        return new MessageBuilder().append(string).build();
    }

    @NonNull
    public static Message from(final MessageEmbed embed) {
        return new MessageBuilder().setEmbeds(embed).build();
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
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final Message message,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                onFail
        );
    }

    // Message
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final Message message,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                null
        );
    }

    // Embed
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final MessageEmbed embed,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                from(embed),
                onSuccess,
                null
        );
    }

    // Embed
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final MessageEmbed embed) {
        sendMessage0(
                channel,
                from(embed),
                null,
                null
        );
    }

    // String
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final String content,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                from(content),
                onSuccess,
                onFail
        );
    }

    // String
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final String content,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                from(content),
                onSuccess,
                null
        );
    }

    // String
    public static void sendMessage(@NonNull final MessageChannel channel, @NonNull final String content) {
        sendMessage0(
                channel,
                from(content),
                null,
                null
        );
    }

    // private
    public static void sendPrivateMessage(@NonNull final User user, @NonNull final String content,
                                          @Nullable final Consumer<Message> onSuccess, @NonNull final Consumer<Throwable> onFail) {
        sendPrivateMessage(user, from(content), onSuccess, onFail);
    }

    // private
    // in Wolfia, it is very important that messages reach their destination, that's why private messages require a failure
    // handler, so that each time a private message is coded a conscious decision is made how a failure should be handled
    public static void sendPrivateMessage(@NonNull final User user, @NonNull final Message message,
                                          @Nullable final Consumer<Message> onSuccess, @NonNull final Consumer<Throwable> onFail) {
        user.openPrivateChannel().queue(
                privateChannel -> sendMessage(privateChannel, message, onSuccess, onFail),
                onFail
        );
    }

    // ********************************************************************************
    //                            Message editing methods
    // ********************************************************************************

    public static void editMessage(@NonNull final Message oldMessage, @NonNull final String newContent) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newContent)
        );
    }

    public static void editMessage(@NonNull final Message oldMessage, @NonNull final MessageEmbed newEmbed) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newEmbed)
        );
    }

    // ********************************************************************************
    //                   Miscellaneous messaging related methods
    // ********************************************************************************

    public static void sendTyping(@NonNull final MessageChannel channel) {
        try {
            channel.sendTyping().queue(
                    null,
                    getJdaRestActionFailureHandler("Could not send typing event in channel " + channel.getId())
            );
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that the message passed in here is actually existing in Discord
    // e.g. dont pass messages in here that were created with a MessageBuilder in our code
    public static void deleteMessage(@NonNull final Message message) {
        try {
            message.delete().queue(
                    null,
                    getJdaRestActionFailureHandler(String.format("Could not delete message %s in channel %s with content%n%s",
                            message.getId(), message.getChannel().getId(), message.getContentRaw()),
                            ErrorResponse.UNKNOWN_MESSAGE) //user deleted their message, dun care
            );
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(message.getChannel(), e);
        }
    }

    // ********************************************************************************
    //                           Class internal methods
    // ********************************************************************************

    //class internal message sending method
    private static void sendMessage0(@NonNull final MessageChannel channel, @NonNull final Message message,
                                     @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        final Consumer<Message> successWrapper = m -> {
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        final Consumer<Throwable> failureWrapper = t -> {
            if (onFail != null) {
                onFail.accept(t);
            } else {
                final String info = String.format("Could not sent message%n%s%nwith %s embeds to channel %s in guild %s",
                        message.getContentRaw(), message.getEmbeds().size(), channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "private");
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            channel.sendMessage(message).queue(successWrapper, failureWrapper);
        } catch (final InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
            if (e.getPermission() == Permission.MESSAGE_EMBED_LINKS) {
                handleInsufficientPermissionsException(channel, e);
            } else {
                //do not call RestActions#handleInsufficientPermissionsException() from here as that will result in a loop
                log.warn("Could not send message with content {} and {} embeds to channel {} due to missing permission {}",
                        message.getContentRaw(), message.getEmbeds().size(), channel.getIdLong(), e.getPermission().getName(), e);
            }
        }
    }

    //class internal editing method
    private static void editMessage0(@NonNull final MessageChannel channel, final long oldMessageId,
                                     @NonNull final Message newMessage) {

        final Consumer<Throwable> failureWrapper = t -> {
            final String info = String.format("Could not edit message %s in channel %s in guild %s with new content %s and %s embeds",
                    oldMessageId, channel.getId(),
                    (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null",
                    newMessage.getContentRaw(), newMessage.getEmbeds().size());
            getJdaRestActionFailureHandler(info).accept(t);
        };

        try {
            channel.editMessageById(oldMessageId, newMessage).queue(null, failureWrapper);
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    private static void handleInsufficientPermissionsException(@NonNull final MessageChannel channel,
                                                               @NonNull final InsufficientPermissionException e) {
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
    public static Consumer<Throwable> getJdaRestActionFailureHandler(final String info, final ErrorResponse... ignored) {
        final LogTheStackException ex = new LogTheStackException();
        return t -> {
            ex.initCause(t);
            if (t instanceof ErrorResponseException) {
                final ErrorResponseException e = (ErrorResponseException) t;
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
