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

package space.npstr.wolfia.domain.maintenance;

import org.springframework.stereotype.Service;
import space.npstr.wolfia.db.HstoreKey;
import space.npstr.wolfia.db.HstoreRepository;

@Service
public class MaintenanceService {

    private final HstoreRepository repository;

    public MaintenanceService(HstoreRepository repository) {
        this.repository = repository;
    }

    public boolean getMaintenanceFlag() {
        String maintenanceFlag = this.repository.get(
                HstoreKey.DEFAULT.NAME,
                HstoreKey.DEFAULT.MAINTENANCE_FLAG,
                Boolean.FALSE.toString()
        ).toCompletableFuture().join();

        return Boolean.parseBoolean(maintenanceFlag);
    }

    public void flipMaintenanceFlag() {
        boolean maintenanceFlag = getMaintenanceFlag();

        this.repository.set(
                HstoreKey.DEFAULT.NAME,
                HstoreKey.DEFAULT.MAINTENANCE_FLAG,
                Boolean.toString(!maintenanceFlag)
        ).toCompletableFuture().join();
    }
}
