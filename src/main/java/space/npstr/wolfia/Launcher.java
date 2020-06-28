/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDAInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import space.npstr.prometheus_extensions.ThreadPoolCollector;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.utils.GitRepoState;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 10.05.18.
 *
 *  //general list of todos etc
 *  //todo rename role pm/dm -> rolecard
 */
@SpringBootApplication(exclude = { //we handle these ourselves
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class Launcher implements ApplicationRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Launcher.class);

    @SuppressWarnings("NullableProblems")
    private static BotContext botContext;

    private final ThreadPoolCollector poolMetrics;
    private final WolfiaConfig wolfiaConfig;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1068"}) //see EagerLoader
    private final EagerLoader eagerLoader;

    public static BotContext getBotContext() {
        return botContext;
    }

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
            if (event instanceof ApplicationFailedEvent) {
                final ApplicationFailedEvent failed = (ApplicationFailedEvent) event;
                log.error("Application failed", failed.getException());
                System.exit(ShutdownHandler.EXIT_CODE_RESTART);
            }
        });
        app.run(args);
    }

    public Launcher(BotContext botContext, ThreadPoolCollector poolMetrics, WolfiaConfig wolfiaConfig,
                    EagerLoader eagerLoader) {
        Launcher.botContext = botContext;
        this.poolMetrics = poolMetrics;
        this.wolfiaConfig = wolfiaConfig;
        this.eagerLoader = eagerLoader;
    }

    @Override
    public void run(final ApplicationArguments args) {
        this.poolMetrics.addPool("restActions", (ScheduledThreadPoolExecutor) RestActions.restService);

        if (this.wolfiaConfig.isDebug())
            log.info("Running DEBUG configuration");
        else
            log.info("Running PRODUCTION configuration");
    }

    @Nonnull
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
