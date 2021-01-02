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

package space.npstr.wolfia.system.togglz;

import org.springframework.stereotype.Component;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;
import space.npstr.wolfia.domain.FeatureFlag;
import space.npstr.wolfia.events.BotStatusLogger;
import space.npstr.wolfia.utils.discord.Emojis;

@Component
public class StatusLoggingStateRepo implements StateRepository {

    private final BotStatusLogger botStatusLogger;

    public StatusLoggingStateRepo(BotStatusLogger botStatusLogger) {
        this.botStatusLogger = botStatusLogger;
    }

    @Override
    public FeatureState getFeatureState(Feature feature) {
        return null;
    }

    @Override
    public void setFeatureState(FeatureState featureState) {
        if (featureState.getFeature() == FeatureFlag.MAINTENANCE) {
            if (featureState.isEnabled()) {
                this.botStatusLogger.log(Emojis.TOOLS, "Maintenance started");
            } else {
                this.botStatusLogger.log(Emojis.CHECKERED_FLAG, "Maintenance finished");
            }
        }
    }
}
