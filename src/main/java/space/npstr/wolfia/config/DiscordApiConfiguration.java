package space.npstr.wolfia.config;

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.sqlsauce.DatabaseWrapper;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.discordwrapper.DiscordEntityProvider;
import space.npstr.wolfia.discordwrapper.JdaDiscordEntityProvider;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.listings.Listings;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

@Configuration
public class DiscordApiConfiguration {

    @Bean
    public DiscordEntityProvider jdaDiscordEntityProvider(ShardManager shardManager) {
        return new JdaDiscordEntityProvider(shardManager);
    }

    @Bean
    public ShardManager shardManager(WolfiaConfig wolfiaConfig, Database database) throws LoginException {
        DatabaseWrapper wrapper = database.getWrapper();
        return new DefaultShardManagerBuilder()
                .setToken(wolfiaConfig.getDiscordToken())
                .setGame(Game.playing(App.GAME_STATUS))
                .addEventListeners(new CommandListener())
                .addEventListeners(new UserMemberCachingListener<>(wrapper, CachedUser.class))
                .addEventListeners(new GuildCachingListener<>(wrapper, CachedGuild.class))
                .addEventListeners(new InternalListener())
                .addEventListeners(new Listings())
                .addEventListeners(new WolfiaGuildListener())
                .setHttpClientBuilder(Wolfia.getDefaultHttpClientBuilder())
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME, CacheFlag.VOICE_STATE))
                .setEnableShutdownHook(false)
                .setAudioEnabled(false)
                .build();
    }
}
