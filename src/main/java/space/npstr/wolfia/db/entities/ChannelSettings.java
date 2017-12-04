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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.wolfia.Wolfia;

import javax.annotation.Nonnull;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 22.06.17.
 * <p>
 * Saves settings on a per level basis. The difference to SetupEntity is this entity containing for of the technical
 * Discord stuff, while SetupEntity should contain purely game related stuff.
 */
@Entity
@Table(name = "settings_channel")
public class ChannelSettings extends SaucedEntity<Long, ChannelSettings> {

    @Id
    @Column(name = "channel_id")
    private long channelId;

    //the role that provides access to the channel
    @Column(name = "access_role_id")
    private long accessRoleId = -1;

    //taglist for this channel, consists of userIds and possibly roleIds
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tags")
    private final Set<Long> tags = new HashSet<>();

    //last time the taglist was posted
    @Column(name = "tag_list_last_used")
    private long tagListLastUsed = -1;

    //minimum minutes between tags
    @Column(name = "tag_cooldown")
    private long tagCooldown = 5;

    @Nonnull
    @Override
    public ChannelSettings setId(final Long id) {
        this.channelId = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return this.channelId;
    }

    public long getAccessRoleId() {
        return this.accessRoleId;
    }

    @Nonnull
    public ChannelSettings setAccessRoleId(final long accessRoleId) {
        this.accessRoleId = accessRoleId;
        return this;
    }

    public Set<Long> getTags() {
        return this.tags;
    }

    @Nonnull
    public ChannelSettings addTag(final long id) {
        this.tags.add(id);
        return this;
    }

    public void addTags(final Collection<Long> ids) {
        this.tags.addAll(ids);
    }

    @Nonnull
    public ChannelSettings removeTag(final long id) {
        this.tags.remove(id);
        return this;
    }

    public void removeTags(final Collection<Long> ids) {
        this.tags.removeAll(ids);
    }

    public long getTagListLastUsed() {
        return this.tagListLastUsed;
    }

    @Nonnull
    public ChannelSettings usedTagList() {
        this.tagListLastUsed = System.currentTimeMillis();
        return this;
    }

    public long getTagCooldown() {
        return this.tagCooldown;
    }

    @Nonnull
    public ChannelSettings setTagCooldown(final long tagCooldown) {
        this.tagCooldown = tagCooldown;
        return this;
    }

    public MessageEmbed getStatus() {
        final EmbedBuilder eb = new EmbedBuilder();
        final TextChannel channel = Wolfia.getTextChannelById(this.channelId);
        if (channel == null) {
            eb.addField("Could not find channel with id " + this.channelId, "", false);
            return eb.build();
        }
        eb.setTitle("Settings for channel #" + channel.getName());
        eb.setDescription("Changes to the settings are reserved for channel moderators.");
        String roleName = "[Not set up]";
        if (this.accessRoleId > 0) {
            final Role accessRole = channel.getGuild().getRoleById(this.accessRoleId);
            if (accessRole == null) {
                roleName = "[Deleted]";
            } else {
                roleName = accessRole.getName();
            }
        }
        eb.addField("Access Role", roleName, true);

        eb.addField("Tag list cooldown", this.tagCooldown + " minutes", true);

        return eb.build();
    }


    @Nonnull
    public static ChannelSettings load(final long channelId) throws DatabaseException {
        return SaucedEntity.load(EntityKey.of(channelId, ChannelSettings.class));
    }
}
