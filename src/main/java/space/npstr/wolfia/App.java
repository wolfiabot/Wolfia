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

package space.npstr.wolfia;

import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.util.ResourceBundle;

/**
 * Created by napster on 26.04.17.
 * <p>
 * Provides some static information about this bot
 */
public class App {

    private static final ResourceBundle props = ResourceBundle.getBundle("app");
    public static final String VERSION = props.getString("version");
    public static final String GROUP_ID = props.getString("groupId");
    public static final String ARTIFACT_ID = props.getString("artifactId");
    public static final String BUILD_NUMBER = props.getString("buildNumber");
    public static final long BUILD_TIME = Long.parseLong(props.getString("buildTime"));

    public static final long OWNER_ID = 166604053629894657L;//Napster
    //https://discordapp.com/oauth2/authorize?client_id=306583221565521921&response_type=code&redirect_uri=https%3A%2F%2Fdiscordapp.com%2Finvite%2FnvcfX3q&permissions=268787777&scope=bot
    public static final String INVITE_LINK = "https://bot.wolfia.party/invite";
    public static final String WOLFIA_LOUNGE_INVITE = "https://discord.gg/nvcfX3q";//https://bot.wolfia.party/join but this doesnt show the embed
    public static final long WOLFIA_LOUNGE_ID = 315944983754571796L;
    public static final String SITE_LINK = "https://wolfia.party";
    public static final String DOCS_LINK = "https://wolfia.party";
    public static final String GITHUB_LINK = "https://github.com/napstr/wolfia";
    public static final String GAME_STATUS = WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER + " | " + SITE_LINK + " | Public β";

    private static ApplicationInfo appInfo;

    private static final String description = "Play Werewolf / Mafia and similar games on Discord!";

    public static void setAppInfo(final ApplicationInfo ai) {
        appInfo = ai;
    }

    public static String getDescription() {
        return appInfo != null ? appInfo.getDescription() : description;
    }

    public static boolean isOwner(final long userId) {
        return OWNER_ID == userId;
    }

    public static boolean isOwner(final User user) {
        return isOwner(user.getIdLong());
    }

    public static boolean isOwner(final Member member) {
        return isOwner(member.getUser());
    }

    public static String getVersionBuild() {
        return VERSION + "_" + BUILD_NUMBER;
    }

}
