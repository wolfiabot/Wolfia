/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.events.BotStatusLogger;
import space.npstr.wolfia.utils.GitRepoState;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 *  //general list of todos etc
 *  //todo rename role pm/dm -> rolecard
 */
@SpringBootApplication
public class Launcher implements ApplicationRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Launcher.class);

    private final ThreadPoolCollector poolMetrics;
    private final WolfiaConfig wolfiaConfig;
    @SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1068"}) //see EagerLoader
    private final EagerLoader eagerLoader;
    private final BotStatusLogger botStatusLogger;
    private final ShardManager shardManager;

    @SuppressWarnings("squid:S106") // printing to sout is fine here
    public static void main(String[] args) {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }

        log.info(getVersionInfo());

        System.setProperty("spring.config.name", "wolfia");
        SpringApplication app = new SpringApplication(Launcher.class);
        app.setAdditionalProfiles("secrets");
        app.addListeners(event -> {
            if (event instanceof ApplicationEnvironmentPreparedEvent) {
                log.info(getVersionInfo());
            }
            if (event instanceof ApplicationFailedEvent failed) {
                log.error("Application failed", failed.getException());
                System.exit(ShutdownHandler.EXIT_CODE_RESTART);
            }
        });
        app.run(args);
    }

    public Launcher(ThreadPoolCollector poolMetrics, WolfiaConfig wolfiaConfig,
                    EagerLoader eagerLoader, BotStatusLogger botStatusLogger, ShardManager shardManager) {
        this.poolMetrics = poolMetrics;
        this.wolfiaConfig = wolfiaConfig;
        this.eagerLoader = eagerLoader;
        this.botStatusLogger = botStatusLogger;
        this.shardManager = shardManager;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.poolMetrics.addPool("restActions", (ScheduledThreadPoolExecutor) RestActions.restService);
        this.botStatusLogger.fireAndForget(Emojis.ROCKET, "Starting...");
        if (this.wolfiaConfig.isDebug())
            log.info("Running DEBUG configuration");
        else
            log.info("Running PRODUCTION configuration");

        while (!allShardsUp()) {
            Thread.sleep(100);
        }
        this.botStatusLogger.fireAndForget(Emojis.ONE_HUNDRED, "All shards connected!");
    }

    private boolean allShardsUp() {
        if (this.shardManager.getShardCache().size() < this.shardManager.getShardsTotal()) {
            return false;
        }

        return this.shardManager.getShardCache().stream().allMatch(shard -> shard.getStatus() == JDA.Status.CONNECTED);
    }

    private static String getVersionInfo() {
        return ART
                + "\n"
                + "\n\tVersion:       " + App.VERSION
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + TextchatUtils.toBerlinTime(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n";
    }

    //########## vanity
    private static final String ART = "\n"
            + "\n                              __"
            + "\n                            .d$$b"
            + "\n                           .' TO$;\\"
            + "\n        Wolfia            /  : TP._;"
            + "\n    Werewolf & Mafia     / _.;  :Tb|"
            + "\n      Discord bot       /   /   ;j$j"
            + "\n                    _.-\"       d$$$$"
            + "\n                  .' ..       d$$$$;"
            + "\n                 /  /P'      d$$$$P. |\\"
            + "\n                /   \"      .d$$$P' |\\^\"l"
            + "\n              .'           `T$P^\"\"\"\"\"  :"
            + "\n          ._.'      _.'                ;"
            + "\n       `-.-\".-'-' ._.       _.-\"    .-\""
            + "\n     `.-\" _____  ._              .-\""
            + "\n    -(.g$$$$$$$b.              .'"
            + "\n      \"\"^^T$$$P^)            .(:"
            + "\n        _/  -\"  /.'         /:/;"
            + "\n     ._.'-'`-'  \")/         /;/;"
            + "\n  `-.-\"..--\"\"   \" /         /  ;"
            + "\n .-\" ..--\"\"        -'          :"
            + "\n ..--\"\"--.-\"         (\\      .-(\\"
            + "\n   ..--\"\"              `-\\(\\/;`"
            + "\n     _.                      :"
            + "\n                             ;`-"
            + "\n                            :\\"
            + "\n                            ;";
}
