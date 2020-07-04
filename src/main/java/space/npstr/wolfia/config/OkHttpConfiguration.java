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

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import space.npstr.prometheus_extensions.OkHttpEventCounter;
import space.npstr.prometheus_extensions.ThreadPoolCollector;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpConfiguration {

    //a general purpose http client builder
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) //do not reuse the builders
    public OkHttpClient.Builder httpClientBuilder(Dispatcher dispatcher) {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .dispatcher(dispatcher)
                .retryOnConnectionFailure(true);
    }

    @Bean
    public Dispatcher okhttpDispatcher(ThreadPoolCollector poolMetrics) {
        Dispatcher dispatcher = new Dispatcher();
        poolMetrics.addPool("okhttpDispatcher", (ThreadPoolExecutor) dispatcher.executorService());
        return dispatcher;
    }

    // default http client that can be used for anything
    @Bean
    public OkHttpClient defaultHttpClient(OkHttpClient.Builder httpClientBuilder) {
        return httpClientBuilder
                .eventListener(new OkHttpEventCounter("default"))
                .build();
    }
}
