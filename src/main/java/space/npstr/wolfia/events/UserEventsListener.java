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

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import space.npstr.wolfia.db.entity.CachedUser;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

/**
 * Created by napster on 28.07.17.
 * <p>
 * This listener keeps track of various user events like leaving guilds, changing nicks, etc
 * This would also be the place to check for users editing their messages (if we wanted to prohibit that) etc
 * <p>
 * Also handles deleted channels
 */
public class UserEventsListener extends ListenerAdapter {


    @Override
    public void onUserNameUpdate(final UserNameUpdateEvent event) {
        final User user = event.getUser();
        CachedUser.get(user.getIdLong())
                .setName(user.getName())
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
    public void onTextChannelDelete(final TextChannelDeleteEvent event) {
        final long channelId = event.getChannel().getIdLong();
        final long guildId = event.getGuild().getIdLong();

        if (Games.get(channelId) != null) {
            DiscordLogger.getLogger().log("%s `%s` Destroying game due to deleted channel **#%s** `%s` in guild **%s** `%s`.",
                    Emojis.BOOM, TextchatUtils.berlinTime(),
                    event.getChannel().getName(), channelId, event.getGuild().getName(), guildId);

            Games.get(channelId).destroy(new UserFriendlyException("Main game channel `%s` in guild `%s` was deleted", channelId, guildId));
        }
    }
}
