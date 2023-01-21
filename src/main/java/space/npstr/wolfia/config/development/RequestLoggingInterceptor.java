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

package space.npstr.wolfia.config.development;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    private static final AtomicLong REQUEST_ID = new AtomicLong();
    private static final String REQUEST_ID_ATTRIBUTE = RequestLoggingInterceptor.class.getName() + ".REQUEST_ID";
    public static final String UNKNOWN_REQUEST_ID = "Unknown request id";

    @Override
    public boolean preHandle(final HttpServletRequest request, final @NonNull HttpServletResponse response,
                             final @NonNull Object handler) {

        long requestId = REQUEST_ID.getAndIncrement();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        log.debug(">>> {} {} {}",
                requestId,
                request.getMethod(),
                request.getRequestURI()
        );

        return true;
    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final @NonNull HttpServletResponse response,
                                final @NonNull Object handler, final Exception ex) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (requestId == null) requestId = UNKNOWN_REQUEST_ID;
        log.debug("<<< {} {} {}",
                requestId,
                response.getStatus(),
                request.getRequestURI()
        );
    }
}
