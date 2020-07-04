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

package space.npstr.wolfia.webapi;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.npstr.wolfia.system.metrics.MetricsRegistry;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Expose Prometheus metrics. Some code copied from Prometheus' own MetricsServlet.
 */
@RestController
@RequestMapping("/metrics")
public class MetricsEndpoint extends BaseEndpoint {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MetricsEndpoint.class);

    private final CollectorRegistry registry;

    //dependency on the registry is generally a good idea due to spring lazy loading
    public MetricsEndpoint(MetricsRegistry metricsRegistry) {
        this.registry = metricsRegistry.getRegistry();
    }

    @GetMapping(produces = TextFormat.CONTENT_TYPE_004)
    public CompletionStage<ResponseEntity<String>> getMetrics(@RequestParam(name = "name[]", required = false) Optional<String[]> includedParam) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new ResponseEntity<>(buildAnswer(includedParam), HttpStatus.OK);
            } catch (IOException e) {
                log.error("Wait what, metrics endpoint blew up", e);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });
    }

    @SuppressWarnings("squid:S3752")
    @RequestMapping("**")
    public CompletionStage<ResponseEntity<String>> catchAll(HttpServletRequest request) {
        return super.logAndCatchAll(request);
    }

    private String buildAnswer(Optional<String[]> includedParamOpt) throws IOException {
        Set<String> params = includedParamOpt
                .map(includedParam -> (Set<String>) new HashSet<>(Arrays.asList(includedParam)))
                .orElseGet(Collections::emptySet);

        Writer writer = new StringWriter();
        try (writer) {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(params));
            writer.flush();
        }

        return writer.toString();
    }
}
