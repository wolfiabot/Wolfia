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
package space.npstr.wolfia.webapi

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import space.npstr.wolfia.system.logger
import space.npstr.wolfia.system.metrics.MetricsRegistry
import java.io.StringWriter
import java.io.Writer

/**
 * Expose Prometheus metrics. Some code copied from Prometheus' own MetricsServlet.
 */
@RestController
@RequestMapping("/metrics")
class MetricsEndpoint(
    metricsRegistry: MetricsRegistry,
) : BaseEndpoint() {

    private val registry: CollectorRegistry = metricsRegistry.registry

    @GetMapping(produces = [TextFormat.CONTENT_TYPE_004])
    fun getMetrics(
        @RequestParam(name = "name[]", required = false) includedParam: Array<String>?
    ): ResponseEntity<String> {

        return try {
            ResponseEntity(buildAnswer(includedParam), HttpStatus.OK)
        } catch (e: Exception) {
            logger().error("Wait what, metrics endpoint blew up", e)
            ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @RequestMapping("**")
    fun catchAll(request: HttpServletRequest): ResponseEntity<String> {
        return super.logAndCatchAll(request)
    }

    private fun buildAnswer(includedParam: Array<String>?): String {
        val params = includedParam?.let { setOf(*it) } ?: emptySet()
        val writer: Writer = StringWriter()
        writer.use {
            TextFormat.write004(writer, registry.filteredMetricFamilySamples(params))
            writer.flush()
        }
        return writer.toString()
    }
}
