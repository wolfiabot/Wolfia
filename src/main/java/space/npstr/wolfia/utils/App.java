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

package space.npstr.wolfia.utils;

import space.npstr.wolfia.Config;
import space.npstr.wolfia.commands.util.HelpCommand;

import java.util.ResourceBundle;

/**
 * Created by napster on 26.04.17.
 * <p>
 * Provides some static information about this bot
 */
public class App {

    private static final ResourceBundle props = ResourceBundle.getBundle("app");
    public static final String VERSION = props.getString("version");
    public static long OWNER_ID = 166604053629894657L;//Napster
    public static String INVITE_LINK = "https://discordapp.com/oauth2/authorize?&client_id=306583221565521921&scope=bot";
    public static String WOLFIA_LOUNGE_INVITE = "https://discord.gg/nvcfX3q";
    public static String WEBSITE = "http://wolfia.space";
    public static String GAME_STATUS = Config.PREFIX + HelpCommand.COMMAND + " | " + WEBSITE;
    public static String DESCRIPTION = "Play Werewolf/Mafia games on Discord!";
}