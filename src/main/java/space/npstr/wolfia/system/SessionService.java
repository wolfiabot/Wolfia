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

package space.npstr.wolfia.system;

import org.springframework.context.event.EventListener;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.domain.privacy.PersonalDataDelete;

import static org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME;

@Service
public class SessionService {

    private final RedisIndexedSessionRepository sessionRepository;

    public SessionService(RedisIndexedSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @EventListener
    public void onDataDelete(PersonalDataDelete dataDelete) {
        String principalName = Long.toString(dataDelete.userId());
        removeAllSessions(principalName);
    }

    private void removeAllSessions(String principalName) {
        var sessions = this.sessionRepository.findByIndexNameAndIndexValue(PRINCIPAL_NAME_INDEX_NAME, principalName);
        for (String sessionId : sessions.keySet()) {
            this.sessionRepository.deleteById(sessionId);
        }
    }
}
