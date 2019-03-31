package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.Guild;
import org.immutables.value.Value;

/**
 * Data container for a discord guild
 */
@Value.Immutable
public interface DiscordGuild {

    static DiscordGuild from(Guild guild) {
        return ImmutableDiscordGuild.builder()
                .id(guild.getIdLong())
                .build();
    }

    long id();

}
