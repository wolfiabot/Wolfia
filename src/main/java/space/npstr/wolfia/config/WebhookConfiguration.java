/*
 * Copyright (C) 2016-2020 the original author or authors
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

package space.npstr.wolfia.config;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.AllowedMentions;
import java.util.concurrent.ScheduledExecutorService;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.wolfia.config.properties.WolfiaConfig;

@Configuration
public class WebhookConfiguration {

    @Bean(destroyMethod = "") //the pool underlying pool gets shut down by our shutdownhandler
    @ConditionalOnProperty("wolfia.botstatus-webhook")
    public WebhookClient botStatusWebhookClient(WolfiaConfig wolfiaConfig, OkHttpClient.Builder httpClientBuilder,
                                                @Qualifier("mainExceptionLoggingExecutor") ScheduledExecutorService executorService) {

        OkHttpClient httpClient = httpClientBuilder
                .eventListener(new OkHttpEventCounter("webhooks"))
                .build();

        return new WebhookClientBuilder(wolfiaConfig.getBotstatusWebhook())
                .setAllowedMentions(AllowedMentions.none())
                .setExecutorService(executorService)
                .setHttpClient(httpClient)
                .build();
    }

}
