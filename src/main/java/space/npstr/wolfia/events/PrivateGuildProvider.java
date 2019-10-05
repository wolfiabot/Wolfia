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

package space.npstr.wolfia.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.PrivateGuild;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class PrivateGuildProvider {

    private static final Logger log = LoggerFactory.getLogger(PrivateGuildProvider.class);

    private final LinkedBlockingQueue<PrivateGuild> availablePrivateGuildQueue = new LinkedBlockingQueue<>();
    private final Database database;

    public PrivateGuildProvider(Database database) {
        this.database = database;

        try {
            List<PrivateGuild> privateGuilds = loadAllPrivateGuilds();
            log.info("{} private guilds loaded", privateGuilds.size());
            this.availablePrivateGuildQueue.addAll(privateGuilds);
        } catch (final DatabaseException e) {
            log.error("Failed to load private guilds, exiting", e);
            System.exit(2);
        }
    }

    public PrivateGuild take() throws InterruptedException {
        return this.availablePrivateGuildQueue.take();
    }

    public Optional<PrivateGuild> poll() {
        return Optional.ofNullable(this.availablePrivateGuildQueue.poll());
    }

    public void add(PrivateGuild privateGuild) {
        availablePrivateGuildQueue.add(privateGuild);
    }

    public List<PrivateGuild> getAllGuilds() {
        return loadAllPrivateGuilds();
    }

    private List<PrivateGuild> loadAllPrivateGuilds() {
        return this.database.getWrapper()
                .selectJpqlQuery("FROM PrivateGuild", null, PrivateGuild.class);
    }
}
