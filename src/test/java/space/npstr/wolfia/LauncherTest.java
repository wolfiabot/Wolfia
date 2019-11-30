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

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.AbstractApplicationContext;
import space.npstr.prometheus_extensions.jda.JdaMetrics;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.CommandHandler;
import space.npstr.wolfia.config.SentryConfiguration;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.domain.oauth2.OAuth2Refresher;
import space.npstr.wolfia.domain.setup.lastactive.AutoOuter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

abstract class LauncherTest extends ApplicationTest {

    @Autowired
    protected AbstractApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private BeanCatcher beanCatcher;

    @Test
    void applicationContextLoads() {
        // ensure event listeners for messages are initiated
        try {
            publisher.publishEvent(mock(MessageReceivedEvent.class));
        } catch (Exception ignored) {}

        // smoke test for some usual & important beans
        assertThatContainsBean("commandHandler", CommandHandler.class);
        assertThatContainsBean("commRegistry", CommRegistry.class);
        assertThatContainsBean("shardManager", ShardManager.class);
        assertThatContainsBean("botContext", BotContext.class);
        assertThatContainsBean("database", Database.class);
        assertThatContainsBean("shutdownHandler", ShutdownHandler.class);
        assertThatContainsBean("sentryConfiguration", SentryConfiguration.class);
        assertThatContainsBean("jdaMetrics", JdaMetrics.class);
        assertThatContainsBean("OAuth2Refresher", OAuth2Refresher.class);
        assertThatContainsBean("autoOuter", AutoOuter.class);
    }

    private void assertThatContainsBean(String name, Class<?> clazz) {
        var beans = this.beanCatcher.getBeans();
        assertThat(beans).containsKey(name);
        assertThat(beans.get(name)).isInstanceOf(clazz);
    }

}
