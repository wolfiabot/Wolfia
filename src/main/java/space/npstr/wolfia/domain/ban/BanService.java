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

package space.npstr.wolfia.domain.ban;

import org.springframework.stereotype.Service;
import space.npstr.wolfia.db.gen.tables.records.BanRecord;
import space.npstr.wolfia.game.definitions.Scope;

import java.util.List;

@Service
public class BanService {

    private final BanRepository repository;

    public BanService(BanRepository repository) {
        this.repository = repository;
    }

    public boolean isBanned(long userId) {
        return this.repository.findOne(userId, Scope.GLOBAL)
                .toCompletableFuture().join()
                .isPresent();
    }

    public void ban(long userId) {
        this.repository.setScope(userId, Scope.GLOBAL)
                .toCompletableFuture().join();
    }

    public void unban(long userId) {
        this.repository.setScope(userId, Scope.NONE)
                .toCompletableFuture().join();
    }

    public List<BanRecord> getActiveBans() {
        return this.repository.findByScope(Scope.GLOBAL)
                .toCompletableFuture().join();
    }

}
