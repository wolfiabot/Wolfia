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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import org.hibernate.annotations.ColumnDefault;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.IEntity;

import javax.persistence.*;
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
public class ChannelSettings implements IEntity {

    @Id
    @Column(name = "channel_id")
    private long channelId;

    //the role that provides access to the channel
    @Column(name = "access_role_id")
    private long accessRoleId = -1;

    //taglist for this channel, consists of userIds and possibly roleIds
    //todo fix fetch types https://www.thoughts-on-java.org/5-ways-to-initialize-lazy-relations-and-when-to-use-them/
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tags")
    private final Set<Long> tags = new HashSet<>();

    //last time the taglist was posted
    @Column(name = "tag_list_last_used")
    @ColumnDefault(value = "0")
    private long tagListLastUsed;

    //minimum minutes between tags
    @Column(name = "tag_cooldown")
    @ColumnDefault(value = "5")
    private long tagCooldown = 5;

    @Override
    public void setId(final long id) {
        this.channelId = id;
    }

    @Override
    public long getId() {
        return this.channelId;
    }

    public long getAccessRoleId() {
        return this.accessRoleId;
    }

    public void setAccessRoleId(final long accessRoleId) {
        this.accessRoleId = accessRoleId;
    }

    public Set<Long> getTags() {
        return this.tags;
    }

    public void addTag(final long id) {
        this.tags.add(id);
    }

    public void addTags(final Collection<Long> ids) {
        this.tags.addAll(ids);
    }

    public void removeTag(final long id) {
        this.tags.remove(id);
    }

    public void removeTags(final Collection<Long> ids) {
        this.tags.removeAll(ids);
    }

    public long getTagListLastUsed() {
        return this.tagListLastUsed;
    }

    public void usedTagList() {
        this.tagListLastUsed = System.currentTimeMillis();
    }

    public long getTagCooldown() {
        return this.tagCooldown;
    }

    public void setTagCooldown(final long tagCooldown) {
        this.tagCooldown = tagCooldown;
    }

    public MessageEmbed getStatus() {
        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Settings for channel #" + Wolfia.jda.getTextChannelById(this.channelId).getName());
        eb.setDescription("Changes to the settings are reserved for channel moderators.");
        String roleName = "[Not set up]";
        if (this.accessRoleId > 0) {
            final Role accessRole = Wolfia.jda.getTextChannelById(this.channelId).getGuild().getRoleById(this.accessRoleId);
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
}
