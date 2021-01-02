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

package space.npstr.wolfia.domain.stats;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import space.npstr.wolfia.domain.privacy.PersonalDataDelete;

@Service
public class StatsService {

    private final StatsRepository statsRepository;

    public StatsService(StatsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    public GameStats recordGameStats(GameStats gameStats) {
        return this.statsRepository.insertGameStats(gameStats)
                .toCompletableFuture().join();
    }

    @EventListener
    public void onDataDelete(PersonalDataDelete dataDelete) {
        anonymize(dataDelete.userId());
    }

    public void anonymize(long userId) {
        this.statsRepository.nullAllPlayerNicknamesofUser(userId).toCompletableFuture().join();
    }
}
