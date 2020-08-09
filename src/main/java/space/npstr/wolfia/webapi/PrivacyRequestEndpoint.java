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

import javax.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import space.npstr.wolfia.domain.privacy.PrivacyRequestService;
import space.npstr.wolfia.domain.privacy.PrivacyResponse;
import space.npstr.wolfia.domain.privacy.PrivacyService;

@RestController
@RequestMapping("/api/privacy")
public class PrivacyRequestEndpoint {

    private final PrivacyRequestService privacyRequestService;
    private final PrivacyService privacyService;

    public PrivacyRequestEndpoint(PrivacyRequestService privacyRequestService, PrivacyService privacyService) {
        this.privacyRequestService = privacyRequestService;
        this.privacyService = privacyService;
    }

    @GetMapping("/request")
    public ResponseEntity<PrivacyResponse> request(@Nullable WebUser user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(privacyRequestService.request(user.id()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> delete(@Nullable WebUser user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        this.privacyService.dataDelete(user.id());
        return ResponseEntity.noContent().build();
    }
}
