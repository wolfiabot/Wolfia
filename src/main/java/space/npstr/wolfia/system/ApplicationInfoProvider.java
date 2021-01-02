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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.ApplicationTeam;
import net.dv8tion.jda.api.entities.TeamMember;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.utils.Memoizer;

/**
 * Fetch the {@link ApplicationInfo} from Discord.
 * The {@link ApplicationInfo} has information such as our bots id, logo, owner, etc.
 * <p>
 * To avoid running too many requests and since the application information does not really change, it memoized.
 */
public class ApplicationInfoProvider {

    private static final AtomicReference<Supplier<ApplicationInfo>> APP_INFO_MEMOIZER = new AtomicReference<>();

    private final ShardManager shardManager;

    public ApplicationInfoProvider(ShardManager shardManager) {
        this.shardManager = shardManager;
    }

    public ApplicationInfo getApplicationInfo() {
        return getMemoizer().get();
    }

    public boolean isOwner(User user) {
        return isOwner(user.getIdLong());
    }

    public boolean isOwner(long userId) {
        return getOwner().getIdLong() == userId;
    }

    public User getOwner() {
        ApplicationInfo appInfo = getApplicationInfo();
        ApplicationTeam team = appInfo.getTeam();
        if (team == null) {
            return appInfo.getOwner();
        }

        TeamMember owner = team.getOwner();
        if (owner == null) {
            return appInfo.getOwner();
        }

        return owner.getUser();
    }

    private Supplier<ApplicationInfo> getMemoizer() {
        var memoizer = APP_INFO_MEMOIZER.get();
        if (memoizer == null) {
            synchronized (ApplicationInfoProvider.class) {
                memoizer = APP_INFO_MEMOIZER.get();
                if (memoizer == null) {
                    memoizer = Memoizer.memoize(
                            () -> this.shardManager.retrieveApplicationInfo().submit().join()
                    );
                    APP_INFO_MEMOIZER.set(memoizer);
                }
            }
        }

        return memoizer;
    }
}
