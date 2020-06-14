/*
 * Copyright (C) 2016-2020 Dennis Neufeld
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

import java.util.List;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import space.npstr.wolfia.webapi.WebUserArgumentResolver;

@Configuration
public class WebApplicationConfig implements WebMvcConfigurer {

    private static final String NOT_FOUND_URL_PATH = "/notFound";

    private final WebUserArgumentResolver webUserArgumentResolver;

    public WebApplicationConfig(WebUserArgumentResolver webUserArgumentResolver) {
        this.webUserArgumentResolver = webUserArgumentResolver;
    }

    /**
     * This is necessary to enable Vue Router's history mode.
     * See https://stackoverflow.com/a/44697497/
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController(NOT_FOUND_URL_PATH).setViewName("forward:/index.html");
    }

    /**
     * This is necessary to enable Vue Router's history mode.
     * See https://stackoverflow.com/a/44697497/
     */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer() {
        return factory -> factory.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, NOT_FOUND_URL_PATH));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this.webUserArgumentResolver);
    }
}
