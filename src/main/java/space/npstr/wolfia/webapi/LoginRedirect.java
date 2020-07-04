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

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller's sole purpose us to redirect to an internal login endpoint that is not exposed under /api
 * (and thus annoying to expose in dev environments to the web frontend server started by yarn).
 */
@RestController
@RequestMapping(LoginRedirect.ROUTE)
public class LoginRedirect {

    public static final String INIT_DISCORD_LOGIN = "/oauth2/authorization/discord";
    public static final String ROUTE = "/public/login";

    @GetMapping
    public ResponseEntity<Void> login() {
        HttpHeaders headers = new HttpHeaders();

        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication existingAuth = securityContext.getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated() && existingAuth.getAuthorities().contains(Authorization.USER)) {
            headers.setLocation(URI.create("/"));
        } else {
            headers.setLocation(URI.create(INIT_DISCORD_LOGIN));
        }
        return new ResponseEntity<>(null, headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    @DeleteMapping
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
