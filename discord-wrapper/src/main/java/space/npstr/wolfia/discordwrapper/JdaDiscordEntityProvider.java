package space.npstr.wolfia.discordwrapper;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import net.dv8tion.jda.core.requests.ErrorResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.common.Exceptions;
import space.npstr.wolfia.discordwrapper.entity.DiscordGuild;
import space.npstr.wolfia.discordwrapper.entity.DiscordMember;
import space.npstr.wolfia.discordwrapper.entity.DiscordPrivateChannel;
import space.npstr.wolfia.discordwrapper.entity.DiscordSelfUser;
import space.npstr.wolfia.discordwrapper.entity.DiscordTextChannel;
import space.npstr.wolfia.discordwrapper.entity.DiscordUser;

import javax.annotation.CheckReturnValue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

/**
 * Created by napster on 05.05.18.
 */
@Profile("!test")
@Component
public class JdaDiscordEntityProvider implements DiscordEntityProvider {

    private final ShardManager shardManager;

    public JdaDiscordEntityProvider(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    @Override
    @CheckReturnValue
    public Optional<DiscordUser> getUserById(long userId) {
        return Optional.ofNullable(this.shardManager.getUserById(userId))
                .map(DiscordUser::from);
    }

    //try getUserById first for a possibly faster resolution
    //NOTE: these might be fake users, and fake user should not be DMed
    @Override
    @CheckReturnValue
    public CompletionStage<Optional<DiscordUser>> retrieveUserById(long userId) {
        return getAnyShard().retrieveUserById(userId).submit()
                .handle((user, throwable) -> {
                    if (throwable != null) {
                        Throwable realCause = Exceptions.unwrap(throwable);
                        if (realCause instanceof ErrorResponseException) {
                            ErrorResponseException ex = (ErrorResponseException) realCause;
                            if (ex.getErrorResponse() == ErrorResponse.UNKNOWN_USER) {
                                //not a real exception, we are expecting a user not to exist when retrieving
                            } else {
                                //Whatever. Do metrics here later
                            }
                        } else {
                            throw new IllegalStateException("Unexpected exception when retrieving user", realCause);
                        }
                    }


                    return Optional.ofNullable(user)
                            .map(DiscordUser::from);
                });
    }

    //NOTE: these might be fake users, and fake user should not be DMed
    @Override
    @CheckReturnValue
    public CompletionStage<Optional<DiscordUser>> getOrRetrieveUserById(long userId) {
        Optional<DiscordUser> user = getUserById(userId);
        if (user.isPresent()) {
            return CompletableFuture.completedStage(user);
        }
        return retrieveUserById(userId);
    }

    @Override
    @CheckReturnValue
    public Optional<DiscordMember> getMember(long userId, long guildId) {
        return Optional.ofNullable(this.shardManager.getGuildById(guildId))
                .flatMap(guild -> Optional.ofNullable(guild.getMemberById(userId)))
                .map(DiscordMember::from);
    }

    @Override
    @CheckReturnValue
    public Optional<DiscordGuild> getGuildById(long guildId) {
        return Optional.ofNullable(this.shardManager.getGuildById(guildId))
                .map(DiscordGuild::from);

    }

    @Override
    @CheckReturnValue
    public Optional<DiscordTextChannel> getTextChannelById(long channelId) {
        return Optional.ofNullable(this.shardManager.getTextChannelById(channelId))
                .map(DiscordTextChannel::from);
    }

    @Override
    @CheckReturnValue
    public Optional<DiscordPrivateChannel> getPrivateChannelById(long channelId) {
        return Optional.ofNullable(this.shardManager.getPrivateChannelById(channelId))
                .map(DiscordPrivateChannel::from);

    }

    @Override
    @CheckReturnValue
    public boolean allShardsUp() {
        if (this.shardManager.getShardCache().size() < this.shardManager.getShardsTotal()) {
            return false;
        }

        return this.shardManager.getShardCache().stream().allMatch(shard -> shard.getStatus() == JDA.Status.CONNECTED);
    }

    @Override
    @CheckReturnValue
    public Optional<DiscordSelfUser> self() {
        return this.shardManager.getShardCache().stream().findAny().map(JDA::getSelfUser)
                .map(DiscordSelfUser::from);
    }

    @CheckReturnValue
    private JDA getAnyShard() {
        return this.shardManager.getShardCache().stream().findAny().orElseThrow(() -> new IllegalStateException("No active shards!"));
    }

    @Override
    @CheckReturnValue
    public long countShards() {
        return this.shardManager.getShardCache().size();
    }

    @Override
    @CheckReturnValue
    public Stream<DiscordGuild> streamGuilds() {
        return this.shardManager.getGuildCache().stream()
                .map(DiscordGuild::from);
    }

    @Override
    @CheckReturnValue
    public long countGuilds() {
        return this.shardManager.getGuildCache().size();
    }

    @Override
    public void shutdown() {
        this.shardManager.shutdown();
    }

    // check guild -> bot users -> discord api
    @Override
    @CheckReturnValue
    public CompletionStage<String> retrieveNick(long userId, Optional<Long> guildIdOpt) {
        return guildIdOpt
                .flatMap(guildId -> getMember(userId, guildId))
                .map(DiscordMember::effectiveName)
                .map(CompletableFuture::completedStage)
                .orElseGet(() -> getOrRetrieveUserById(userId)
                        .thenApply(userOpt -> userOpt.map(DiscordUser::name).orElse("Unknown User")) //TODO extract string into variable
                );
    }
}
