/*
 * Copyright (C) 2017-2019 Dennis Neufeld
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Common base class for endpoints
 */
public abstract class BaseEndpoint {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseEndpoint.class);

    protected static final ResponseEntity<String> NOT_FOUND = ResponseEntity.notFound().build();

    protected CompletionStage<ResponseEntity<String>> logAndCatchAll(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();
        Iterable<String> acceptHeaders = () -> request.getHeaders(HttpHeaders.ACCEPT).asIterator();
        String acceptableMediaTypes = String.join(", ", acceptHeaders);
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);

        log.info("Catch all triggered: {} {} {}: {} {}: {}",
                method, path, HttpHeaders.ACCEPT, acceptableMediaTypes, HttpHeaders.CONTENT_TYPE, contentType);
        return CompletableFuture.completedStage(NOT_FOUND);
    }
}
