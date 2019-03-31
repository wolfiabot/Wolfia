package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.TextChannel;
import org.immutables.value.Value;

/**
 * Data container for a discord text channel
 */
@Value.Immutable
public interface DiscordTextChannel {

    static DiscordTextChannel from(TextChannel textChannel) {
        return ImmutableDiscordTextChannel.builder()
                .id(textChannel.getIdLong())
                .build();
    }

    long id();
}
