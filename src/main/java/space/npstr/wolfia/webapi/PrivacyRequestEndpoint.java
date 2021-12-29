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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final ObjectMapper objectMapper;

    public PrivacyRequestEndpoint(PrivacyRequestService privacyRequestService, PrivacyService privacyService) {
        this.privacyRequestService = privacyRequestService;
        this.privacyService = privacyService;
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .registerModule(new JavaTimeModule())
        ;
    }

    @GetMapping("/request")
    public ResponseEntity<String> request(@Nullable WebUser user) throws JsonProcessingException {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        PrivacyResponse response = privacyRequestService.request(user.id());
        return ResponseEntity.ok(this.objectMapper.writeValueAsString(response));
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
