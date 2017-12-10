/*
 * Copyright (C) 2017 Dennis Neufeld
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

package space.npstr.wolfia.db.entities;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.IEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.LogTheStackException;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by napster on 17.06.17.
 * <p>
 * A private guild
 */
@Entity
@Table(name = "private_guilds")
public class PrivateGuild extends ListenerAdapter implements IEntity<Long, PrivateGuild> {

    private static final Logger log = LoggerFactory.getLogger(PrivateGuild.class);
    private static final String WOLF_ROLE_NAME = "Wolf";
    //using a static scope for the lock since entities, while representing the same data, may be distinct objects
    private static final Object usageLock = new Object();

    @NaturalId //unique constraint
    @Column(name = "private_guild_number")
    private int privateGuildNumber;

    @Id
    @Column(name = "guild_id", nullable = false)
    private long guildId;

    @Transient
    private boolean inUse = false;

    @Transient
    private final List<Long> allowedUsers = new ArrayList<>();

    @Transient
    private long currentChannelId = -1;

    //for Hibernate/JPA and creating entities
    public PrivateGuild() {
    }

    public PrivateGuild(final int privateGuildNumber, final long guildId) {
        this.privateGuildNumber = privateGuildNumber;
        this.guildId = guildId;
        this.inUse = false;
    }

    public int getPrivateGuildNumber() {
        return this.privateGuildNumber;
    }

    @Nonnull
    @Override
    public PrivateGuild setId(final Long id) {
        this.guildId = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return this.guildId;
    }

    @Nonnull
    @Override
    public Class<PrivateGuild> getClazz() {
        return PrivateGuild.class;
    }

    public boolean inUse() {
        return this.inUse;
    }


    public static boolean isPrivateGuild(@Nonnull final Guild guild) throws DatabaseException {
        final EntityKey<Long, PrivateGuild> key = EntityKey.of(guild.getIdLong(), PrivateGuild.class);
        return Wolfia.getDbWrapper().getEntity(key) != null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.privateGuildNumber;
        result = prime * result + (int) (this.guildId ^ (this.guildId >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof PrivateGuild)) {
            return false;
        }
        final PrivateGuild pg = (PrivateGuild) obj;
        return this.privateGuildNumber == pg.privateGuildNumber && this.guildId == pg.guildId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != this.guildId) {
            return;
        }

        final Member joined = event.getMember();
        //kick the joined user if they aren't on the allowed list
        if (!this.allowedUsers.contains(joined.getUser().getIdLong())) {
            final Consumer whenDone = aVoid -> event.getGuild().getController().kick(joined).queue(null, RestActions.defaultOnFail());
            final String message = String.format("You are not allowed to join private guild #%s currently.", this.privateGuildNumber);
            RestActions.sendPrivateMessage(joined.getUser(), message, whenDone, whenDone);
            return;
        }

        final Role wolf = RoleAndPermissionUtils.getOrCreateRole(event.getGuild(), WOLF_ROLE_NAME).complete();
        event.getGuild().getController().addRolesToMember(event.getMember(), wolf).queue(
                aVoid -> RestActions.sendMessage(Wolfia.fetchTextChannel(this.currentChannelId),
                        event.getMember().getAsMention() + ", welcome to wolf chat!")
        );
    }

    public void beginUsage(final Collection<Long> wolfUserIds) {
        synchronized (usageLock) {
            if (this.inUse) {
                throw new IllegalStateException("Can't begin the usage of a private guild #" + this.privateGuildNumber + " that is being used already");
            }
            this.inUse = true;
        }

        try {
            final Guild g = fetchThisGuild();

            cleanUpMembers();
            this.allowedUsers.addAll(wolfUserIds);

            //set up a fresh channel
            final TextChannel wolfChannel = (TextChannel) g.getController().createTextChannel("wolfchat")
                    .reason("Preparing private guild for a game").complete();
            this.currentChannelId = wolfChannel.getIdLong();

            //send new user joining messages to the fresh channel
            g.getManager().setSystemChannel(wolfChannel).queue(null, RestActions.defaultOnFail());

            //give the wolfrole access to it
            RoleAndPermissionUtils.grant(wolfChannel, RoleAndPermissionUtils.getOrCreateRole(g, WOLF_ROLE_NAME).complete(),
                    Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue(null, RestActions.defaultOnFail());
        } catch (final Exception e) {
            endUsage();
            throw new RuntimeException("Could not begin the usage of private guild #" + this.privateGuildNumber, e);
        }
    }

    //kick everyone, except guild owner and bots
    private void cleanUpMembers() {
        final Guild g = fetchThisGuild();
        this.allowedUsers.clear();
        g.getMembers().stream().filter(m -> !m.isOwner() && !m.getUser().isBot()).forEach(m -> g.getController().kick(m).queue(null, RestActions.defaultOnFail()));
    }

    public void endUsage() {
        synchronized (usageLock) {
            if (!this.inUse) {
                throw new IllegalStateException("Can't end the usage of a private guild #" + this.privateGuildNumber + " that is not in use ");
            }
            cleanUpMembers();
            try {//complete() in here to catch errors
                //revoke all invites
                for (final TextChannel channel : fetchThisGuild().getTextChannels()) {
                    final List<Invite> invites = channel.getInvites().complete();
                    invites.forEach(i -> i.delete().complete());
                }
                final TextChannel tc = Wolfia.getTextChannelById(this.currentChannelId);
                if (tc != null) {
                    tc.delete().reason("Cleaning up private guild after game ended").complete();
                } else {
                    log.error("Did not find channel {} in private guild #{} to delete it.",
                            this.currentChannelId, this.privateGuildNumber);
                }
            } catch (final Exception e) {
                log.error("Exception while deleting channel {} in private guild #{} {}", this.currentChannelId,
                        this.privateGuildNumber, this.guildId, e);
                return;//leave the private guild in a "broken state", this can be later fixed manually through eval
            }
            this.inUse = false;
            Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.add(this);
        }
    }

    public String getInvite() {
        final Guild g = fetchThisGuild();
        final TextChannel channel = g.getTextChannelById(this.currentChannelId);
        return TextchatUtils.getOrCreateInviteLinkForGuild(g, channel,
                () -> log.error("Could not create invite to private guild #{}, id {}", this.privateGuildNumber, this.guildId));
    }

    public long getChannelId() {
        return this.currentChannelId;
    }

    //this method assumes that the id itself is legit and not a mistake and we are member of this private guild
    // it is an attempt to improve the occasional inconsistency of discord which makes looking up entities a gamble
    // the main feature being the @Nonnull return contract, over the @Nullable contract of looking the entity up in JDA
    @Nonnull
    private Guild fetchThisGuild() {
        Guild g = Wolfia.getGuildById(this.guildId);
        while (g == null) {
            log.error("Could not find private guild #{} with id {}, trying in a moment",
                    this.privateGuildNumber, this.guildId, new LogTheStackException());
            try {
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            g = Wolfia.getGuildById(this.guildId);
        }
        return g;
    }
}
