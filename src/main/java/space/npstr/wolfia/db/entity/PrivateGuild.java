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

package space.npstr.wolfia.db.entity;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.hibernate.annotations.NaturalId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandHandler;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.db.IEntity;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;

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
public class PrivateGuild extends ListenerAdapter implements IEntity {

    private static final Logger log = LoggerFactory.getLogger(PrivateGuild.class);
    private static final String WOLF_ROLE_NAME = "Wolf";

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

    @Override
    public void setId(final long id) {
        this.guildId = id;
    }

    @Override
    public long getId() {
        return this.guildId;
    }

    public boolean inUse() {
        return this.inUse;
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

        //kick everyone who isn't allowed
        if (!this.allowedUsers.contains(event.getMember().getUser().getIdLong())) {
            final Consumer whenDone = aVoid -> event.getGuild().getController().kick(event.getMember()).queue(null, Wolfia.defaultOnFail);
            Wolfia.handlePrivateOutputMessage(event.getMember().getUser().getIdLong(), whenDone, whenDone,
                    "You are not allowed to join private guild #%s currently.", this.privateGuildNumber);
            return;
        }

        final Role wolf = RoleAndPermissionUtils.getOrCreateRole(event.getGuild(), WOLF_ROLE_NAME).complete();
        event.getGuild().getController().addRolesToMember(event.getMember(), wolf).queue(
                aVoid -> Wolfia.handleOutputMessage(this.currentChannelId, "%s, welcome to wolf chat!", event.getMember().getAsMention()),
                Wolfia.defaultOnFail
        );
    }

    //todo the checks here are pretty much a duplication of the checks in the CommandListener, resolve that
    @Override
    public void onMessageReceived(final MessageReceivedEvent event) {
        final long received = System.currentTimeMillis();
        //ignore private channels
        if (event.getPrivateChannel() != null) {
            return;
        }

        //ignore guilds that are not this one
        if (event.getGuild().getIdLong() != this.guildId) {
            return;
        }

        //ignore messages not starting with the prefix (prefix is accepted case insensitive)
        final String raw = event.getMessage().getRawContent();
        if (!raw.toLowerCase().startsWith(Config.PREFIX.toLowerCase())) {
            return;
        }

        //ignore bot accounts generally
        if (event.getAuthor().isBot()) {
            return;
        }

        //bot should ignore itself
        if (event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        final CommandParser.CommandContainer commandInfo = CommandParser.parse(raw, event, received);
        CommandListener.getCommandExecutor().execute(() -> CommandHandler.handleCommand(commandInfo, this::filter));
    }

    private boolean filter(final BaseCommand command) {
        //allow only nk related commands
        return command instanceof NightkillCommand || command instanceof VoteCountCommand || command instanceof UnvoteCommand;
    }

    public synchronized void beginUsage(final Collection<Long> wolfUserIds) {
        if (this.inUse) {
            throw new IllegalStateException("Can't begin the usage of a private guild #" + this.privateGuildNumber + " that is being used already");
        }
        this.inUse = true;

        try {
            final Guild g = Wolfia.jda.getGuildById(this.guildId);
            cleanUpMembers();
            this.allowedUsers.addAll(wolfUserIds);

            //set up a fresh channel
            final Channel wolfChannel = g.getController().createTextChannel("wolfchat").complete();
            this.currentChannelId = wolfChannel.getIdLong();

            //give the wolfrole access to it
            RoleAndPermissionUtils.grant(wolfChannel, RoleAndPermissionUtils.getOrCreateRole(g, WOLF_ROLE_NAME).complete(),
                    Permission.MESSAGE_WRITE, Permission.MESSAGE_READ).queue(null, Wolfia.defaultOnFail);
        } catch (final Exception e) {
            endUsage();
            throw new RuntimeException("Could not begin the usage of private guild #" + this.privateGuildNumber, e);
        }
    }

    //kick everyone, except guild owner and bots
    private void cleanUpMembers() {
        final Guild g = Wolfia.jda.getGuildById(this.guildId);
        this.allowedUsers.clear();
        g.getMembers().stream().filter(m -> !m.isOwner() && !m.getUser().isBot()).forEach(m -> g.getController().kick(m).queue(null, Wolfia.defaultOnFail));
    }

    public synchronized void endUsage() {
        if (!this.inUse) {
            throw new IllegalStateException("Can't end the usage of a private guild #" + this.privateGuildNumber + " that is not in use ");
        }
        cleanUpMembers();
        try {//complete() in here to catch errors
            //revoke all invites
            for (final TextChannel channel : Wolfia.jda.getGuildById(this.guildId).getTextChannels()) {
                final List<Invite> invites = channel.getInvites().complete();
                invites.forEach(i -> i.delete().complete());
            }
            Wolfia.jda.getTextChannelById(this.currentChannelId).delete().complete();
        } catch (final Exception e) {
            log.error("Exception while deleting channel {} in private guild #{} {}", this.currentChannelId, this.privateGuildNumber, this.guildId, e);
            return;//leave the private guild in a "broken state", this can be later fixed manually through eval
        }
        this.inUse = false;
        Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.add(this);
    }

    public String getInvite() {
        final Guild g = Wolfia.jda.getGuildById(this.guildId);
        TextChannel channel = g.getTextChannelById(this.currentChannelId);
        if (channel == null) channel = g.getPublicChannel();
        return TextchatUtils.getOrCreateInviteLink(channel, () -> {
            throw new RuntimeException("Could not create invite to private guild " + this.guildId);
        });
    }

    public long getChannelId() {
        return this.currentChannelId;
    }
}
