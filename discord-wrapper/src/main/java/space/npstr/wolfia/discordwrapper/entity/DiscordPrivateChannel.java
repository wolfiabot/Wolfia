package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.PrivateChannel;
import org.immutables.value.Value;

/**
 * Data container for a discord private channel
 */
@Value.Immutable
public interface DiscordPrivateChannel {

    static DiscordPrivateChannel from(PrivateChannel privateChannel) {
        return ImmutableDiscordPrivateChannel.builder()
                .id(privateChannel.getIdLong())
                .build();
    }

    long id();
}
