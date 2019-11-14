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
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.repository.FeatureState;
import space.npstr.wolfia.domain.FeatureFlag;

@Service
public class MaintenanceService {

    private final FeatureManager featureManager;

    public MaintenanceService(FeatureManager featureManager) {
        this.featureManager = featureManager;
    }

    public boolean getMaintenanceFlag() {
        return this.featureManager.isActive(FeatureFlag.MAINTENANCE);
    }

    public void flipMaintenanceFlag() {
        FeatureState featureState = this.featureManager.getFeatureState(FeatureFlag.MAINTENANCE);
        featureState.setEnabled(!featureState.isEnabled());
        this.featureManager.setFeatureState(featureState);
    }
}
