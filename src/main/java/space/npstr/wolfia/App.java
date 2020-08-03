/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.ResourceBundle;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;

/**
 * Provides some static information about this bot
 */
public class App {

    private static final ResourceBundle props = ResourceBundle.getBundle("META-INF/build-info");
    public static final String VERSION = props.getString("build.version");

    /**
     * See {@link space.npstr.wolfia.webapi.InviteEndpoint}
     */
    public static final String INVITE_LINK = "https://bot.wolfia.party/invite";
    /**
     * Using https://bot.wolfia.party/join does not show the embed in Discord, so sometimes the direct link is helpful
     */
    public static final String WOLFIA_LOUNGE_INVITE = "https://discord.gg/nvcfX3q";
    public static final long WOLFIA_LOUNGE_ID = 315944983754571796L;
    public static final String SITE_LINK = "https://wolfia.party";
    public static final String DOCS_LINK = "https://wolfia.party";
    public static final String PRIVACY_LINK = "https://bot.wolfia.party/privacy";
    public static final String GITHUB_LINK = "https://github.com/wolfiabot/wolfia";
    public static final String GAME_STATUS = WolfiaConfig.DEFAULT_PREFIX + HelpCommand.TRIGGER + " | " + SITE_LINK + " | Public β";

    public static final long MODERATOR_ROLE_ID = 340205944866865162L;
    public static final long SETUP_MANAGER_ROLE_ID = 328603453544988672L;
    public static final long DEVELOPER_ROLE_ID = 713041074674860083L;

    private App() {}
}
