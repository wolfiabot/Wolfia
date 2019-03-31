package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.User;
import org.immutables.value.Value;

/**
 * Data container for a discord user
 */
@Value.Immutable
public interface DiscordUser {

    static DiscordUser from(User user) {
        return ImmutableDiscordUser.builder()
                .id(user.getIdLong())
                .name(user.getName())
                .build();
    }

    long id();

    String name();

}
