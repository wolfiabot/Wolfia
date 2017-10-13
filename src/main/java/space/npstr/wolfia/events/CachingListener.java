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
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent;
import net.dv8tion.jda.core.events.user.UserAvatarUpdateEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.db.entities.EGuild;

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
        Wolfia.executor.submit(() -> CachedUser.cache(Wolfia.getInstance().dbWrapper, event.getUser()));
    }

    @Override
    public void onGuildMemberNickChange(final GuildMemberNickChangeEvent event) {
        Wolfia.executor.submit(() -> CachedUser.cache(Wolfia.getInstance().dbWrapper, event.getMember()));
    }

    //todo a last seen kinda thing for signup auto outing

    @Override
    public void onUserAvatarUpdate(final UserAvatarUpdateEvent event) {
        Wolfia.executor.submit(() -> CachedUser.cache(Wolfia.getInstance().dbWrapper, event.getUser()));
    }

    @Override
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        Wolfia.executor.submit(() -> CachedUser.cache(Wolfia.getInstance().dbWrapper, event.getMember()));
    }

    @Override
    public void onGuildUpdateName(final GuildUpdateNameEvent event) {
        final Guild guild = event.getGuild();
        Wolfia.executor.submit(() -> EGuild.load(Wolfia.getInstance().dbWrapper, guild.getIdLong())
                .set(guild)
                .save()
        );
    }

    @Override
    public void onGuildUpdateIcon(final GuildUpdateIconEvent event) {
        final Guild guild = event.getGuild();
        Wolfia.executor.submit(() -> EGuild.load(Wolfia.getInstance().dbWrapper, guild.getIdLong())
                .set(guild)
                .save());
    }
}
