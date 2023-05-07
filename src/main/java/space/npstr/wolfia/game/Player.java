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

package space.npstr.wolfia.game;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.springframework.lang.Nullable;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.domain.UserCache;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Item;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserNotPresentException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Representing a player in a game
 */
public class Player {

    public static final String UNKNOWN_NAME = "Unknown User";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Player.class);

    public final long userId;
    public final long channelId;
    public final long guildId; //guild where the game is running in
    public final Alignments alignment;
    public final Roles role;
    public final int number;

    public final List<Item> items = new CopyOnWriteArrayList<>();

    private String rolePm = "This player has no role pm.";
    private boolean isAlive = true;

    public Player(long userId, long channelId, long guildId, Alignments alignment,
                  Roles role, int number) {
        this.userId = userId;
        this.channelId = channelId;
        this.guildId = guildId;
        this.alignment = alignment;
        this.role = role;
        this.number = number;
    }

    public long getUserId() {
        return this.userId;
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public boolean isDead() {
        return !this.isAlive;
    }

    public String getRolePm() {
        return this.rolePm;
    }

    public void setRolePm(String rolePm) {
        this.rolePm = rolePm;
    }

    public boolean isBaddie() {
        return this.alignment == Alignments.WOLF;
    }

    public boolean isGoodie() {
        return this.alignment == Alignments.VILLAGE;
    }

    public boolean hasItemOfType(Item.ItemType item) {
        return this.items.stream().anyMatch(i -> i.itemType.equals(item));
    }

    /**
     * @return the discord user (global) name of this player.
     * May return a placeholder for unknown users in weird edge cases
     */
    public String getName() {
        try {
            return Launcher.getBotContext().getUserCache().user(this.userId).getName();
        } catch (Exception e) {
            log.warn("Failed to fetch user {} name", this.userId, e);
            return UNKNOWN_NAME;
        }
    }

    /**
     * @return the discord user nick name of this player in the guild of where the game is happening.
     * May return a placeholder for unknown users in weird edge cases
     */
    public String getNick() {
        try {
            return Launcher.getBotContext().getUserCache().user(this.userId).getEffectiveName(this.guildId);
        } catch (Exception e) {
            log.warn("Failed to fetch user {} nick", this.userId, e);
            return UNKNOWN_NAME;
        }
    }

    /**
     * @return Name of this player in the form of **name** OR **nick** aka ** name**, where the name is this discord
     * users global name and the nick is the optional nick in the guild of the main game channel
     * May return a placeholder for unknown users in weird edge cases
     */
    public String bothNamesFormatted() {
        String name = UNKNOWN_NAME;
        String nick = UNKNOWN_NAME;
        try {
            UserCache userCache = Launcher.getBotContext().getUserCache();
            name = userCache.user(this.userId).getName();
            nick = userCache.user(this.userId).getNick(this.guildId).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to fetch user {} name and nick", this.userId, e);
        }
        return formatNameAndNick(name, nick);
    }

    private String formatNameAndNick(String name, @Nullable String nick) {
        if (name.equals(nick) || nick == null) {
            return "**" + TextchatUtils.escapeMarkdown(name) + "**";
        } else {
            return "**" + TextchatUtils.escapeMarkdown(nick) + "** aka **" + TextchatUtils.escapeMarkdown(name) + "**";
        }
    }

    public String asMention() {
        return TextchatUtils.userAsMention(this.userId);
    }

    /**
     * @return an emoji representing role and alignment of this player
     */
    public String getCharakterEmoji() {
        //role specific ones
        if (this.role == Roles.COP) {
            return Emojis.MAGNIFIER;
        } else if (this.role == Roles.SANTA) {
            return Emojis.SANTA;
        }
        //alignment specific ones
        if (this.alignment == Alignments.WOLF) {
            return Emojis.SPY;
        }
        return Emojis.COWBOY;
    }

    public String numberAsEmojis() {
        String numberStr = Integer.toString(this.number);
        StringBuilder result = new StringBuilder();
        for (char c : numberStr.toCharArray()) {
            int i = Integer.parseInt(c + "");
            result.append(Emojis.NUMBERS.get(i));
        }
        return result.toString();
    }

    public void kill() throws IllegalGameStateException {
        if (!this.isAlive) {
            throw new IllegalGameStateException("Can't kill a dead player");
        }
        this.isAlive = false;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.userId ^ (this.userId >>> 32));
        result = prime * result + (int) (this.channelId ^ (this.channelId >>> 32));
        result = prime * result + (int) (this.guildId ^ (this.guildId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Player)) return false;
        Player other = (Player) obj;
        return this.userId == other.userId && this.channelId == other.channelId;
    }

    public void sendMessage(String content, Consumer<Throwable> onFail) {
        sendMessage(RestActions.from(content), onFail);
    }

    public void sendMessage(MessageEmbed embed, Consumer<Throwable> onFail) {
        sendMessage(RestActions.from(embed), onFail);
    }

    /**
     * Send a private message to the user behind this player. May supply the failure handler with a UserNotPresentException
     * if the user is not present in the bot
     */
    public void sendMessage(Message message, Consumer<Throwable> onFail) {
        User user = Launcher.getBotContext().getShardManager().getUserById(this.userId);
        if (user != null) {
            RestActions.sendPrivateMessage(user, message, null, onFail);
        } else {
            onFail.accept(new UserNotPresentException(this.userId));
        }
    }
}
