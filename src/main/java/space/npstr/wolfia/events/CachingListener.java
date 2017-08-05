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

package space.npstr.wolfia.events;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.core.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.db.entity.CachedUser;
import space.npstr.wolfia.db.entity.EGuild;

/**
 * Created by napster on 28.07.17.
 * <p>
 * This listener keeps track of various user and guild events like leaving guilds, changing nicks or names, etc and
 * keeps them in sync with our database
 * <p>
 */
public class CachingListener extends ListenerAdapter {


    @Override
    public void onUserNameUpdate(final UserNameUpdateEvent event) {
        final User user = event.getUser();
        CachedUser.get(user.getIdLong())
                .set(user)
                .save();
    }

    @Override
    public void onGuildMemberNickChange(final GuildMemberNickChangeEvent event) {
        final Member member = event.getMember();
        CachedUser.get(member.getUser().getIdLong())
                .set(member)
                .save();
    }

    //todo a last seen kinda thing for signup auto outing

    @Override
    public void onUserAvatarUpdate(final UserAvatarUpdateEvent event) {
        CachedUser.get(event.getUser().getIdLong())
                .set(event.getUser())
                .save();
    }

    @Override
    public void onGuildUpdateName(final GuildUpdateNameEvent event) {
        final Guild guild = event.getGuild();
        EGuild.get(guild.getIdLong())
                .set(guild)
                .save();
    }

    @Override
    public void onGuildUpdateIcon(final GuildUpdateIconEvent event) {
        final Guild guild = event.getGuild();
        EGuild.get(guild.getIdLong())
                .set(guild)
                .save();
    }
}
