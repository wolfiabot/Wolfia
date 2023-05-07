/*
 * Copyright (C) 2016-2023 the original author or authors
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

package space.npstr.wolfia.domain.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.system.metrics.MetricsRegistry;

@Component
public class PrivateRoomQueue {

    private static final Logger log = LoggerFactory.getLogger(PrivateRoomQueue.class);

    private final List<ManagedPrivateRoom> allManagedRooms = new ArrayList<>();
    private final LinkedBlockingQueue<ManagedPrivateRoom> availablePrivateRoomQueue = new LinkedBlockingQueue<>();

    public PrivateRoomQueue(PrivateRoomService privateRoomService) {
        List<ManagedPrivateRoom> privateRooms = privateRoomService.findAll().stream()
                .map(pr -> new ManagedPrivateRoom(pr, this))
                .collect(Collectors.toList());
        log.info("{} private rooms loaded", privateRooms.size());
        this.allManagedRooms.addAll(privateRooms);
        this.availablePrivateRoomQueue.addAll(privateRooms);
    }

    public int availableRoomsAmount() {
        return availablePrivateRoomQueue.size();
    }

    @EventListener
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        getAllManagedRooms().forEach(room -> room.onGuildMemberJoin(event));
    }

    public List<ManagedPrivateRoom> getAllManagedRooms() {
        return Collections.unmodifiableList(this.allManagedRooms);
    }

    public ManagedPrivateRoom take() throws InterruptedException {
        ManagedPrivateRoom room = this.availablePrivateRoomQueue.take();
        MetricsRegistry.availablePrivateRooms.set(this.availableRoomsAmount());
        if (room.isInUse()) {
            log.warn("Got a room that is still in use: {}", room);
        }
        return room;
    }

    public Optional<ManagedPrivateRoom> poll() {
        ManagedPrivateRoom room = this.availablePrivateRoomQueue.poll();
        if (room != null) {
            if (room.isInUse()) {
                log.warn("Got a room that is still in use: {}", room);
            }
            MetricsRegistry.availablePrivateRooms.set(this.availableRoomsAmount());
        }
        return Optional.ofNullable(room);
    }

    public void putBack(ManagedPrivateRoom room) {
        if (room.isInUse()) {
            log.warn("Putting back a room that is still in use: {}", room);
        }
        boolean alreadyInQueue = this.availablePrivateRoomQueue.stream()
                .anyMatch(it -> it.getGuildId() == room.getGuildId());
        if (alreadyInQueue) {
            log.warn("Tried to put room into queue that is already there: {}", room);
            return;
        }

        this.availablePrivateRoomQueue.add(room);
        MetricsRegistry.availablePrivateRooms.set(this.availableRoomsAmount());
    }

    public ManagedPrivateRoom add(PrivateRoom privateRoom) {
        ManagedPrivateRoom managedPrivateRoom = new ManagedPrivateRoom(privateRoom, this);
        this.allManagedRooms.add(managedPrivateRoom);
        this.availablePrivateRoomQueue.add(managedPrivateRoom);
        MetricsRegistry.availablePrivateRooms.set(this.availableRoomsAmount());
        return managedPrivateRoom;
    }
}
