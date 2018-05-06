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

package space.npstr.wolfia.game;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Item;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.UserNotPresentException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Created by napster on 05.07.17.
 * <p>
 * Representing a player in a game
 */
@Slf4j
public class Player {

    public final long userId;
    public final long channelId;
    public final long guildId; //guild where the game is running in
    @Nonnull
    public final Alignments alignment;
    @Nonnull
    public final Roles role;
    public final int number;

    public final List<Item> items = new CopyOnWriteArrayList<>();

    @Nonnull
    private String rolePm = "This player has no role pm.";
    private boolean isAlive = true;

    public Player(final long userId, final long channelId, final long guildId, @Nonnull final Alignments alignment,
                  @Nonnull final Roles role, final int number) {
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

    @Nonnull
    public String getRolePm() {
        return this.rolePm;
    }

    public void setRolePm(@Nonnull final String rolePm) {
        this.rolePm = rolePm;
    }

    public boolean isBaddie() {
        return this.alignment == Alignments.WOLF;
    }

    public boolean isGoodie() {
        return this.alignment == Alignments.VILLAGE;
    }

    public boolean hasItemOfType(@Nonnull final Item.Items item) {
        return this.items.stream().anyMatch(i -> i.item.equals(item));
    }

    /**
     * @return the discord user (global) name of this player.
     * May return a placeholder for unknown users in weird edge cases
     */
    @Nonnull
    public String getName() {
        final User user = Wolfia.getUserById(this.userId);
        if (user != null) {
            return user.getName();
        }
        try {
            return CachedUser.load(this.userId).getName();
        } catch (final DatabaseException e) {
            return DiscordUser.UNKNOWN_NAME;
        }
    }

    /**
     * @return the discord user nick name of this player in the guild of where the game is happening.
     * May return a placeholder for unknown users in weird edge cases
     */
    @Nonnull
    public String getNick() {
        final Guild guild = Wolfia.getGuildById(this.guildId);
        if (guild != null) {
            final Member member = guild.getMemberById(this.userId);
            if (member != null) {
                return member.getEffectiveName();
            }
        }
        try {
            final CachedUser cu = CachedUser.load(this.userId);
            final String nick = cu.getNick(this.guildId);
            if (nick != null) {
                return nick;
            } else {
                return cu.getName();
            }
        } catch (final DatabaseException e) {
            return DiscordUser.UNKNOWN_NAME;
        }
    }

    /**
     * @return Name of this player in the form of **name** OR **nick** aka ** name**, where the name is this discord
     * users global name and the nick is the optional nick in the guild of the main game channel
     * May return a placeholder for unknown users in weird edge cases
     */
    @Nonnull
    public String bothNamesFormatted() {
        final Guild guild = Wolfia.getGuildById(this.guildId);
        if (guild != null) {
            final Member member = guild.getMemberById(this.userId);
            if (member != null) {
                return formatNameAndNick(member.getUser().getName(), member.getNickname());
            }
        }

        String name = DiscordUser.UNKNOWN_NAME;
        String nick = DiscordUser.UNKNOWN_NAME;
        try {
            final CachedUser cu = CachedUser.load(this.userId);
            name = cu.getName();
            nick = cu.getNick(this.guildId);
        } catch (final DatabaseException e) {
            log.error("Db blew up why looking up cache user {}", this.userId, e);
        }
        return formatNameAndNick(name, nick);
    }

    @Nonnull
    private String formatNameAndNick(@Nonnull final String name, @Nullable final String nick) {
        if (name.equals(nick) || nick == null) {
            return "**" + TextchatUtils.escapeMarkdown(name) + "**";
        } else {
            return "**" + TextchatUtils.escapeMarkdown(nick) + "** aka **" + TextchatUtils.escapeMarkdown(name) + "**";
        }
    }

    @Nonnull
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
        final String numberStr = Integer.toString(this.number);
        final StringBuilder result = new StringBuilder();
        for (final char c : numberStr.toCharArray()) {
            final int i = Integer.parseInt(c + "");
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
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.userId ^ (this.userId >>> 32));
        result = prime * result + (int) (this.channelId ^ (this.channelId >>> 32));
        result = prime * result + (int) (this.guildId ^ (this.guildId >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Player)) return false;
        final Player other = (Player) obj;
        return this.userId == other.userId && this.channelId == other.channelId;
    }

    public void sendMessage(@Nonnull final String content, @Nonnull final Consumer<Throwable> onFail) {
        sendMessage(RestActions.from(content), onFail);
    }


    public void sendMessage(@Nonnull final MessageEmbed embed, @Nonnull final Consumer<Throwable> onFail) {
        sendMessage(RestActions.from(embed), onFail);
    }

    /**
     * Send a private message to the user behind this player. May supply the failure handler with a UserNotPresentException
     * if the user is not present in the bot
     */
    public void sendMessage(@Nonnull final Message message, @Nonnull final Consumer<Throwable> onFail) {
        final User user = Wolfia.getUserById(this.userId);
        if (user != null) {
            RestActions.sendPrivateMessage(user, message, null, onFail);
        } else {
            onFail.accept(new UserNotPresentException(this.userId));
        }
    }
}
