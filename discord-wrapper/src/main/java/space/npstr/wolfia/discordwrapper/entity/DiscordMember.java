package space.npstr.wolfia.discordwrapper.entity;

import net.dv8tion.jda.core.entities.Member;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Data container for a discord member
 */
@Value.Immutable
public interface DiscordMember {

    static DiscordMember from(Member member) {
        return ImmutableDiscordMember.builder()
                .user(DiscordUser.from(member.getUser()))
                .guildId(member.getGuild().getIdLong())
                .nickname(Optional.ofNullable(member.getNickname()))
                .build();
    }

    default String effectiveName() {
        return nickname().orElse(user().name());
    }

    DiscordUser user();

    long guildId();

    Optional<String> nickname();
}
