package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.SelfUser;
import org.immutables.value.Value;

/**
 * Data container for the discord self user
 */
@Value.Immutable
public interface DiscordSelfUser {

    static DiscordSelfUser from(SelfUser selfUser) {
        return ImmutableDiscordSelfUser.builder()
                .id(selfUser.getIdLong())
                .build();
    }

    long id();


}
