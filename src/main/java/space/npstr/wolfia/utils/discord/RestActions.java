package space.npstr.wolfia.utils.discord;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.requests.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entities.stats.MessageOutputStats;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Everything related to sending RestActions
 * Copy pastad and adjusted from FredBoat where I wrote this for
 */
public class RestActions {

    private static final Logger log = LoggerFactory.getLogger(RestActions.class);

    //this is needed for when we absolutely don't care about a rest action failing (use this only after good consideration!)
    // because if we pass null for a failure handler to JDA it uses a default handler that results in a warning/error level log
    public static final Consumer<Throwable> NOOP_THROWABLE_HANDLER = __ -> {
    };

    //use this to schedule rest actions whenever queueAfter() or similar JDA methods would be used
    // this makes it way easier to track stats + handle failures of such delayed RestActions
    // instead of implementing a ton of overloaded methods in this class
    public static final ScheduledExecutorService restService = Executors.newScheduledThreadPool(10,
            runnable -> new Thread(runnable, "rest-actions-scheduler"));


    // ********************************************************************************
    //       Thread local handling and providing of Messages and Embeds builders
    // ********************************************************************************

    //instead of creating hundreds of MessageBuilder and EmbedBuilder objects we're going to reuse the existing ones, on
    // a per-thread scope
    // this makes sense since the vast majority of message processing in the main JDA threads

    private static final ThreadLocal<MessageBuilder> threadLocalMessageBuilder = ThreadLocal.withInitial(MessageBuilder::new);
    private static final ThreadLocal<EmbedBuilder> threadLocalEmbedBuilder = ThreadLocal.withInitial(EmbedBuilder::new);

    @Nonnull
    public static MessageBuilder getMessageBuilder() {
        return threadLocalMessageBuilder.get().clear();
    }

    @Nonnull
    public static EmbedBuilder getEmbedBuilder() {
        return threadLocalEmbedBuilder.get().clear();
    }

    //May not be an empty string, as MessageBuilder#build() will throw an exception
    @Nonnull
    public static Message from(final String string) {
        return getMessageBuilder().append(string).build();
    }

    @Nonnull
    public static Message from(final MessageEmbed embed) {
        return getMessageBuilder().setEmbed(embed).build();
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
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final Message message,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                onFail
        );
    }

    // Message
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final Message message,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                message,
                onSuccess,
                null
        );
    }

    // Message
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final Message message) {
        sendMessage0(
                channel,
                message,
                null,
                null
        );
    }

    // Embed
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final MessageEmbed embed,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                from(embed),
                onSuccess,
                onFail
        );
    }

    // Embed
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final MessageEmbed embed,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                from(embed),
                onSuccess,
                null
        );
    }

    // Embed
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final MessageEmbed embed) {
        sendMessage0(
                channel,
                from(embed),
                null,
                null
        );
    }

    // String
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final String content,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        sendMessage0(
                channel,
                from(content),
                onSuccess,
                onFail
        );
    }

    // String
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final String content,
                                   @Nullable final Consumer<Message> onSuccess) {
        sendMessage0(
                channel,
                from(content),
                onSuccess,
                null
        );
    }

    // String
    public static void sendMessage(@Nonnull final MessageChannel channel, @Nonnull final String content) {
        sendMessage0(
                channel,
                from(content),
                null,
                null
        );
    }

    // private
    public static void sendPrivateMessage(@Nonnull final User user, @Nonnull final MessageEmbed embed,
                                          @Nullable final Consumer<Message> onSuccess, @Nonnull final Consumer<Throwable> onFail) {
        sendPrivateMessage(user, from(embed), onSuccess, onFail);
    }

    // private
    public static void sendPrivateMessage(@Nonnull final User user, @Nonnull final String content,
                                          @Nullable final Consumer<Message> onSuccess, @Nonnull final Consumer<Throwable> onFail) {
        sendPrivateMessage(user, from(content), onSuccess, onFail);
    }

    // private
    // in Wolfia, it is very important that messages reach their destination, that's why private messages require a failure
    // handler, so that each time a private message is coded a conscious decision is made how a failure should be handled
    public static void sendPrivateMessage(@Nonnull final User user, @Nonnull final Message message,
                                          @Nullable final Consumer<Message> onSuccess, @Nonnull final Consumer<Throwable> onFail) {
        user.openPrivateChannel().queue(
                privateChannel -> {
//                    Metrics.successfulRestActions.labels("openPrivateChannel").inc();
                    sendMessage(privateChannel, message, onSuccess, onFail);

                },
                onFail
        );
    }

    // ********************************************************************************
    //                            Message editing methods
    // ********************************************************************************

    /**
     * @param oldMessage The message to be edited
     * @param newMessage The message to be set
     * @param onSuccess  Optional success handler
     * @param onFail     Optional exception handler
     */
    public static void editMessage(@Nonnull final Message oldMessage, @Nonnull final Message newMessage,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                newMessage,
                onSuccess,
                onFail
        );
    }

    public static void editMessage(@Nonnull final Message oldMessage, @Nonnull final Message newMessage) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                newMessage,
                null,
                null
        );
    }

    public static void editMessage(@Nonnull final Message oldMessage, @Nonnull final String newContent) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newContent),
                null,
                null
        );
    }

    public static void editMessage(@Nonnull final Message oldMessage, @Nonnull final MessageEmbed newEmbed) {
        editMessage0(
                oldMessage.getChannel(),
                oldMessage.getIdLong(),
                from(newEmbed),
                null,
                null
        );
    }


    public static void editMessage(@Nonnull final MessageChannel channel, final long oldMessageId, @Nonnull final Message newMessage,
                                   @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        editMessage0(
                channel,
                oldMessageId,
                newMessage,
                onSuccess,
                onFail
        );
    }

    public static void editMessage(@Nonnull final MessageChannel channel, final long oldMessageId, @Nonnull final Message newMessage) {
        editMessage0(
                channel,
                oldMessageId,
                newMessage,
                null,
                null
        );
    }

    public static void editMessage(@Nonnull final MessageChannel channel, final long oldMessageId, @Nonnull final String newContent) {
        editMessage0(
                channel,
                oldMessageId,
                from(newContent),
                null,
                null
        );
    }

    // ********************************************************************************
    //                   Miscellaneous messaging related methods
    // ********************************************************************************

    public static void sendTyping(@Nonnull final MessageChannel channel) {
        try {
            channel.sendTyping().queue(
//                    __ -> Metrics.successfulRestActions.labels("sendTyping").inc(),
                    null,
                    getJdaRestActionFailureHandler("Could not send typing event in channel " + channel.getId())
            );
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that all the messages are from the channel you provide
    public static void deleteMessages(@Nonnull final TextChannel channel, @Nonnull final Collection<Message> messages) {
        if (!messages.isEmpty()) {
            try {
                channel.deleteMessages(messages).queue(
//                        __ -> Metrics.successfulRestActions.labels("bulkDeleteMessages").inc(),
                        null,
                        getJdaRestActionFailureHandler(String.format("Could not bulk delete %s messages in channel %s",
                                messages.size(), channel.getId()))
                );
            } catch (final InsufficientPermissionException e) {
                handleInsufficientPermissionsException(channel, e);
            }
        }
    }

    public static void deleteMessageById(@Nonnull final MessageChannel channel, final long messageId) {
        try {
            channel.getMessageById(messageId).queue(
                    message -> {
//                        Metrics.successfulRestActions.labels("getMessageById").inc();
                        deleteMessage(message);
                    },
                    NOOP_THROWABLE_HANDLER //prevent logging an error if that message could not be found in the first place
            );
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(channel, e);
        }
    }

    //make sure that the message passed in here is actually existing in Discord
    // e.g. dont pass messages in here that were created with a MessageBuilder in our code
    public static void deleteMessage(@Nonnull final Message message) {
        try {
            message.delete().queue(
//                    __ -> Metrics.successfulRestActions.labels("deleteMessage").inc(),
                    null,
                    getJdaRestActionFailureHandler(String.format("Could not delete message %s in channel %s with content\n%s",
                            message.getId(), message.getChannel().getId(), message.getContentRaw()),
                            ErrorResponse.UNKNOWN_MESSAGE) //user deleted their message, dun care
            );
        } catch (final InsufficientPermissionException e) {
            handleInsufficientPermissionsException(message.getChannel(), e);
        }
    }

    @Nonnull
    public static EmbedBuilder addFooter(@Nonnull final EmbedBuilder eb, @Nonnull final Member author) {
        return eb.setFooter(author.getEffectiveName(), author.getUser().getAvatarUrl());
    }

    // ********************************************************************************
    //                           Class internal methods
    // ********************************************************************************

    //class internal message sending method
    private static void sendMessage0(@Nonnull final MessageChannel channel, @Nonnull final Message message,
                                     @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        final Consumer<Message> successWrapper = m -> {
//            Metrics.successfulRestActions.labels("sendMessage").inc();
            Wolfia.executor.submit(() -> Wolfia.getDbWrapper().persist(new MessageOutputStats(m)));
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        final Consumer<Throwable> failureWrapper = t -> {
            if (onFail != null) {
                onFail.accept(t);
            } else {
                final String info = String.format("Could not sent message\n%s\nwith %s embeds to channel %s in guild %s",
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
    private static void editMessage0(@Nonnull final MessageChannel channel, final long oldMessageId, @Nonnull final Message newMessage,
                                     @Nullable final Consumer<Message> onSuccess, @Nullable final Consumer<Throwable> onFail) {
        final Consumer<Message> successWrapper = m -> {
//            Metrics.successfulRestActions.labels("editMessage").inc();
            if (onSuccess != null) {
                onSuccess.accept(m);
            }
        };
        final Consumer<Throwable> failureWrapper = t -> {
            if (onFail != null) {
                onFail.accept(t);
            } else {
                final String info = String.format("Could not edit message %s in channel %s in guild %s with new content %s and %s embeds",
                        oldMessageId, channel.getId(),
                        (channel instanceof TextChannel) ? ((TextChannel) channel).getGuild().getIdLong() : "null",
                        newMessage.getContentRaw(), newMessage.getEmbeds().size());
                getJdaRestActionFailureHandler(info).accept(t);
            }
        };

        try {
            channel.editMessageById(oldMessageId, newMessage).queue(successWrapper, failureWrapper);
        } catch (final InsufficientPermissionException e) {
            if (onFail != null) {
                onFail.accept(e);
            }
            handleInsufficientPermissionsException(channel, e);
        }
    }

    private static void handleInsufficientPermissionsException(@Nonnull final MessageChannel channel,
                                                               @Nonnull final InsufficientPermissionException e) {
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
//                Metrics.failedRestActions.labels(Integer.toString(e.getErrorCode())).inc();
                if (Arrays.asList(ignored).contains(e.getErrorResponse())
                        || e.getErrorCode() == -1 //socket timeout, fuck those
                        ) {
                    return;
                }
            }
            log.error("{}\n{}", info, t.getMessage(), ex);
        };
    }
}
