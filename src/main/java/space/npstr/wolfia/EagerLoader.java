/*
 * Copyright (C) 2016-2025 the original author or authors
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

import org.springframework.stereotype.Component;
import space.npstr.prometheus_extensions.jda.JdaMetrics;
import space.npstr.wolfia.commands.CommandHandler;
import space.npstr.wolfia.domain.oauth2.OAuth2Refresher;
import space.npstr.wolfia.domain.setup.lastactive.AutoOuter;
import space.npstr.wolfia.game.GameResources;

/**
 * Stuff that is required in the context but gets missed by Spring's lazy loading.
 */
@Component
@SuppressWarnings({"FieldCanBeLocal", "unused", "squid:S1068"})
public class EagerLoader {

    private final ShutdownHandler shutdownHandler;
    private final JdaMetrics jdaMetrics;
    private final OAuth2Refresher oAuth2Refresher;
    private final AutoOuter autoOuter;
    private final GameResources gameResources;
    private final CommandHandler commandHandler;

    public EagerLoader(ShutdownHandler shutdownHandler, JdaMetrics jdaMetrics, OAuth2Refresher oAuth2Refresher,
                       AutoOuter autoOuter, GameResources gameResources, CommandHandler commandHandler) {

        this.shutdownHandler = shutdownHandler;
        this.jdaMetrics = jdaMetrics;
        this.oAuth2Refresher = oAuth2Refresher;
        this.autoOuter = autoOuter;
        this.gameResources = gameResources;
        this.commandHandler = commandHandler;
    }
}
