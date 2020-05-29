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

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;

/**
 * Created by napster on 07.01.18.
 * <p>
 * Handles special events for the official Wolfia guild
 */
@Component
public class WolfiaGuildListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WolfiaGuildListener.class);

    public static final long SPAM_CHANNEL_ID = 388705267916734465L; //#spam-and-bot-commands
    public static final long ALPHAWOLVES_ROLE_ID = 326147400790179840L; //@AlphaWolves
    public static final long GAME_CATEGORY_ID = 361189457266737152L; //game category parent channel id
    public static final long RULES_CHANNEL_ID = 326353722701774848L; //#rules

    private static final String WELCOME_PATTERN = "Welcome %s to the **Wolfia Lounge**! Please take a moment and read "
            + "<#326353722701774848> for information, rules, and how to play games. Also check <704040767026626621> "
            + "for our scheduled games: you can give yourself the roles for the games you want to be notified about! "
            + "Don't forget to enjoy and have fun! " + Emojis.WINK;

    @EventListener
    public void onGuildMemberJoin(final GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != App.WOLFIA_LOUNGE_ID
                || Launcher.getBotContext().getWolfiaConfig().isDebug()) {
            return;
        }

        //send greetings to spam channel
        final TextChannel spam = event.getGuild().getTextChannelById(SPAM_CHANNEL_ID);
        if (spam != null) {
            RestActions.sendMessage(spam, String.format(WELCOME_PATTERN, event.getMember().getAsMention()));
        } else {
            log.warn("Did the spam channel disappear in the Wolfia Lounge?");
        }
    }
}
