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

package space.npstr.wolfia.domain.room;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Adds behaviour and mutable data to a {@link PrivateRoom}
 * <p>
 * Even though this is a new class it contains mostly behaviour from the legacy 'PrivateGuild' class and
 * needs to be cleaned up as well as made more robust.
 */
public class ManagedPrivateRoom extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ManagedPrivateRoom.class);

    private static final String WOLF_ROLE_NAME = "Wolf";
    //using a static scope for the lock since entities, while representing the same data, may be distinct objects
    private static final Object usageLock = new Object();

    private final PrivateRoom privateRoom;
    private final PrivateRoomQueue privateRoomQueue;

    private boolean inUse = false;
    private final Set<Long> allowedUsers = new HashSet<>();
    private long currentChannelId = -1;

    public ManagedPrivateRoom(PrivateRoom privateRoom, PrivateRoomQueue privateRoomQueue) {
        this.privateRoom = privateRoom;
        this.privateRoomQueue = privateRoomQueue;
    }

    public long getGuildId() {
        return this.privateRoom.getGuildId();
    }

    public int getNumber() {
        return this.privateRoom.getNumber();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != this.privateRoom.getGuildId()) {
            return;
        }

        final Member joined = event.getMember();
        //kick the joined user if they aren't on the allowed list
        if (!this.allowedUsers.contains(joined.getUser().getIdLong())) {
            final Consumer whenDone = __ -> event.getGuild().kick(joined).queue(null, RestActions.defaultOnFail());
            final String message = String.format("You are not allowed to join private guild #%s currently.", this.privateRoom.getNumber());
            String users = this.allowedUsers.stream().map(Number::toString).collect(Collectors.joining(", "));
            log.debug("Denied user {}, allowed users are {}", joined.getUser().getId(), users);
            RestActions.sendPrivateMessage(joined.getUser(), message, whenDone, whenDone);
            return;
        }

        final Role wolf = RoleAndPermissionUtils.getOrCreateRole(event.getGuild(), WOLF_ROLE_NAME).complete();
        event.getGuild().addRoleToMember(event.getMember(), wolf).queue(
                aVoid -> {
                    ShardManager shardManager = requireNonNull(event.getJDA().getShardManager());
                    TextChannel textChannel = shardManager.getTextChannelById(this.currentChannelId);
                    if (textChannel != null) {
                        RestActions.sendMessage(textChannel, event.getMember().getAsMention() + ", welcome to wolf chat!");
                    }
                }
        );
    }

    public void beginUsage(final Collection<Long> wolfUserIds) {
        synchronized (usageLock) {
            if (this.inUse) {
                throw new IllegalStateException("Can't begin the usage of a private guild #" + this.privateRoom.getNumber() + " that is being used already");
            }
            this.inUse = true;
        }

        try {
            cleanUpMembers();
            this.allowedUsers.addAll(wolfUserIds);
            final Guild g = fetchThisGuild();

            //set up a fresh channel
            final TextChannel wolfChannel = g.createTextChannel("wolfchat")
                    .reason("Preparing private guild for a game").complete();
            this.currentChannelId = wolfChannel.getIdLong();

            //send new user joining messages to the fresh channel
            g.getManager().setSystemChannel(wolfChannel).queue(null, RestActions.defaultOnFail());

            //give the wolfrole access to it
            RoleAndPermissionUtils.grant(wolfChannel, RoleAndPermissionUtils.getOrCreateRole(g, WOLF_ROLE_NAME).complete(),
                    Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue(null, RestActions.defaultOnFail());
        } catch (final Exception e) {
            endUsage();
            throw new RuntimeException("Could not begin the usage of private guild #" + this.privateRoom.getNumber(), e);
        }
    }

    //kick everyone, except guild owner and bots
    private void cleanUpMembers() {
        this.allowedUsers.clear();
        final Guild g = fetchThisGuild();
        g.getMembers().stream()
                .filter(member -> !member.isOwner())
                .filter(member -> !member.getUser().isBot())
                .forEach(member -> g.kick(member).queue(null, RestActions.defaultOnFail()));
    }

    public void endUsage() {
        synchronized (usageLock) {
            if (!this.inUse) {
                throw new IllegalStateException("Can't end the usage of a private guild #" + this.privateRoom.getNumber() + " that is not in use ");
            }
            cleanUpMembers();
            try {//complete() in here to catch errors
                //revoke all invites
                for (final TextChannel channel : fetchThisGuild().getTextChannels()) {
                    final List<Invite> invites = channel.retrieveInvites().complete();
                    invites.forEach(i -> i.delete().complete());
                }
                final TextChannel tc = Launcher.getBotContext().getShardManager().getTextChannelById(this.currentChannelId);
                if (tc != null) {
                    tc.delete().reason("Cleaning up private guild after game ended").complete();
                } else {
                    log.error("Did not find channel {} in private guild #{} to delete it.",
                            this.currentChannelId, this.privateRoom.getNumber());
                }
            } catch (final Exception e) {
                log.error("Exception while deleting channel {} in private guild #{} {}", this.currentChannelId,
                        this.privateRoom.getNumber(), this.privateRoom.getGuildId(), e);
                return;//leave the private guild in a "broken state", this can be later fixed manually through eval
            }
            this.inUse = false;
            this.privateRoomQueue.putBack(this);
        }
    }

    public String getInvite() {
        final Guild g = fetchThisGuild();
        final TextChannel channel = g.getTextChannelById(this.currentChannelId);
        return TextchatUtils.getOrCreateInviteLinkForGuild(g, channel,
                () -> log.error("Could not create invite to private guild #{}, id {}", this.privateRoom.getNumber(), this.privateRoom.getGuildId()));
    }

    public long getChannelId() {
        return this.currentChannelId;
    }

    //this method assumes that the id itself is legit and not a mistake and we are member of this private guild
    // it is an attempt to improve the occasional inconsistency of discord which makes looking up entities a gamble
    // the main feature being the @Nonnull return contract, over the @Nullable contract of looking the entity up in JDA
    @Nonnull
    private Guild fetchThisGuild() {
        ShardManager shardManager = Launcher.getBotContext().getShardManager();
        Guild guild = shardManager.getGuildById(this.privateRoom.getGuildId());
        int attempts = 0;
        while (guild == null) {
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Failed to fetch private guild #" + this.privateRoom.getNumber());
            }
            log.error("Could not find private guild #{} with id {}, trying in a moment",
                    this.privateRoom.getNumber(), this.privateRoom.getGuildId(), new LogTheStackException());
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            guild = shardManager.getGuildById(this.privateRoom.getGuildId());
        }
        return guild;
    }
}
