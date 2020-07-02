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

package space.npstr.wolfia.config;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * This just exists to get rid off a nasty Undertow warning
 * <p>
 * io.undertow.websockets.jsr: "UT026010: Buffer pool was not set on WebSocketDeploymentInfo, the default pool will be used"
 */
@Configuration
public class WebsocketConfig {

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowWebSocketServletWebServerCustomizer() {
        return new UndertowWebSocketServletWebServerCustomizer();
    }

    private static class UndertowWebSocketServletWebServerCustomizer
            implements WebServerFactoryCustomizer<UndertowServletWebServerFactory>, Ordered, DisposableBean {

        // Optimal size for direct buffers is 16kB according to Undertow docs
        // http://undertow.io/undertow-docs/undertow-docs-2.0.0/index.html#the-undertow-buffer-pool
        private final ByteBufferPool buffers = new DefaultByteBufferPool(true, 16 * 1024, 100, 12);

        @Override
        public void customize(UndertowServletWebServerFactory factory) {
            factory.addDeploymentInfoCustomizers(deploymentInfo -> {
                WebSocketDeploymentInfo info = (WebSocketDeploymentInfo) deploymentInfo.getServletContextAttributes().get(WebSocketDeploymentInfo.ATTRIBUTE_NAME);
                if (info == null) {
                    info = new WebSocketDeploymentInfo();
                    deploymentInfo.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
                }

                info.setBuffers(this.buffers);
            });
        }

        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public void destroy() {
            this.buffers.close();
        }
    }
}
