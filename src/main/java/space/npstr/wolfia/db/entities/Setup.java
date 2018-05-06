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

package space.npstr.wolfia.db.entities;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.fp.types.EntityKey;
import space.npstr.sqlsauce.hibernate.types.BasicType;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommRegistry;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by napster on 29.05.17.
 * <p>
 * holds persistent setup information on a channel scope
 */
@Slf4j
@Entity
@Table(name = "setup")
public class Setup extends SaucedEntity<Long, Setup> {

    @Id
    @Column(name = "channel_id", nullable = false)
    private long channelId;

    @Type(type = "hash-set-basic")
    @BasicType(Long.class)
    @Column(name = "inned_users", nullable = false, columnDefinition = "bigint[]")
    @ColumnDefault("array[]::bigint[]")
    private final HashSet<Long> innedUsers = new HashSet<>();

    //one of the values of the Games.GAMES enum
    @Column(name = "game", columnDefinition = "text", nullable = false)
    private String game = "";

    //optional mode, for example CLASSIC or WILD for Popcorn games
    @Column(name = "mode", columnDefinition = "text", nullable = false)
    private String mode = "";

    //day length in milliseconds
    @Column(name = "day_length", nullable = false)
    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10);

    //some basic getters/setters
    @Nonnull
    @Override
    public Setup setId(final Long id) {
        this.channelId = id;
        return this;
    }

    @Nonnull
    @Override
    public Long getId() {
        return this.channelId;
    }

    public Games getGame() {
        return Games.valueOf(this.game);
    }

    @Nonnull
    public Setup setGame(@Nonnull final Games game) {
        this.game = game.name();
        return this;
    }

    public GameInfo.GameMode getMode() {
        return GameInfo.GameMode.valueOf(this.mode);
    }

    @Nonnull
    public Setup setMode(@Nonnull final GameInfo.GameMode mode) throws IllegalArgumentException {
        if (!Games.getInfo(getGame()).getSupportedModes().contains(mode)) {
            final String message = String.format("Game %s does not support mode %s", getGame().name(), mode);
            throw new IllegalArgumentException(message);
        }
        this.mode = mode.name();
        return this;
    }

    public long getDayLengthMillis() {
        return this.dayLengthMillis;
    }

    @Nonnull
    public Setup setDayLength(final long dayLength, @Nonnull final TimeUnit timeUnit) {
        this.dayLengthMillis = timeUnit.toMillis(dayLength);
        return this;
    }

    //create a fresh setup; default game is Popcorn, default mode is Wild
    public Setup() {
        this.setGame(Games.POPCORN);
        this.setMode(Games.getInfo(Games.POPCORN).getDefaultMode());
        this.setDayLength(5, TimeUnit.MINUTES);
    }

    @Nonnull
    public static EntityKey<Long, Setup> key(final long channelId) {
        return EntityKey.of(channelId, Setup.class);
    }

    public boolean isInned(final long userId) {
        return this.innedUsers.contains(userId);
    }

    public Setup inUser(final long userId) throws DatabaseException {
        //cache any inning users
        CachedUser.cache(Wolfia.getDatabase().getWrapper(), getThisChannel().getGuild().getMemberById(userId), CachedUser.class);
        this.innedUsers.add(userId);
        return this;
    }

    public boolean outUser(final long userId) throws DatabaseException {
        return this.innedUsers.remove(userId);
    }

    private void cleanUpInnedPlayers() throws DatabaseException {
        //did they leave the guild?
        final Set<Long> toBeOuted = new HashSet<>();
        final Guild g = getThisChannel().getGuild();
        this.innedUsers.forEach(userId -> {
            if (g.getMemberById(userId) == null) {
                toBeOuted.add(userId);
            }
        });
        for (final Long userId : toBeOuted) {
            outUser(userId);
        }

        //todo whenever time based ins are a thing, this is probably the place to check them
    }

    public MessageEmbed getStatus() throws DatabaseException {
        //clean up first
        cleanUpInnedPlayers();

        final NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
        final TextChannel channel = Wolfia.getTextChannelById(this.channelId);
        if (channel == null) {
            neb.addField("Could not find channel with id " + this.channelId, "", false);
            return neb.build();
        }
        neb.setTitle("Setup for channel #" + channel.getName());
        neb.setDescription(Games.get(this.channelId) == null ? "Game has **NOT** started yet." : "Game has started.");

        //games
        final StringBuilder possibleGames = new StringBuilder();
        Arrays.stream(Games.values()).forEach(g -> possibleGames.append(g == getGame() ? "`[x]` " : "`[ ]` ").append(g.textRep).append("\n"));
        neb.addField("Game", possibleGames.toString(), true);

        //modes
        final StringBuilder possibleModes = new StringBuilder();
        Games.getInfo(getGame()).getSupportedModes().forEach(mode -> possibleModes.append(mode.equals(getMode()) ? "`[x]` " : "`[ ]` ").append(mode).append("\n"));
        neb.addField("Mode", possibleModes.toString(), true);

        //day length
        neb.addField("Day length", TextchatUtils.formatMillis(this.dayLengthMillis), true);

        //accepted player numbers
        neb.addField("Accepted players",
                Games.getInfo(getGame()).getAcceptablePlayerNumbers(getMode()),
                true);

        //inned players
        final NiceEmbedBuilder.ChunkingField inned = new NiceEmbedBuilder.ChunkingField("Inned players (" + this.innedUsers.size() + ")", true);
        final List<String> formatted = this.innedUsers.stream().map(userId -> TextchatUtils.userAsMention(userId) + ", ").collect(Collectors.toList());
        if (!formatted.isEmpty()) {
            String lastOne = formatted.remove(formatted.size() - 1);
            lastOne = lastOne.substring(0, lastOne.length() - 2); //remove the last ", "
            formatted.add(lastOne);
        }
        inned.addAll(formatted);
        neb.addField(inned);

        return neb.build();
    }

    //needs to be synchronized so only one incoming command at a time can be in here
    public synchronized boolean startGame(final long commandCallerId)
            throws IllegalGameStateException, DatabaseException {
        final TextChannel channel = Wolfia.fetchTextChannel(this.channelId);
        //need to synchronize on a class level due to this being an entity object that may be loaded twice from the database
        synchronized (Setup.class) {
            if (MaintenanceCommand.getMaintenanceFlag() || Wolfia.isShuttingDown()) {
                RestActions.sendMessage(channel, "The bot is under maintenance. Please try starting a game later.");
                return false;
            }

            if (!this.innedUsers.contains(commandCallerId)) {
                RestActions.sendMessage(channel, String.format("%s, only players that inned can start the game! Say `%s` to join!",
                        TextchatUtils.userAsMention(commandCallerId), Config.PREFIX + CommRegistry.COMM_TRIGGER_IN));
                return false;
            }

            //is there a game running already in this channel?
            if (Games.get(this.channelId) != null) {
                RestActions.sendMessage(channel, TextchatUtils.userAsMention(commandCallerId)
                        + ", there is already a game going on in this channel!");
                return false;
            }

            final Game game;
            try {
                game = this.getGame().clazz.getConstructor().newInstance();
            } catch (final IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalGameStateException("Internal error, could not create the specified game.", e);
            }

            cleanUpInnedPlayers();
            final Set<Long> inned = new HashSet<>(this.innedUsers);
            if (!game.isAcceptablePlayerCount(inned.size(), getMode())) {
                RestActions.sendMessage(channel, String.format(
                        "There aren't enough (or too many) players signed up! Please use `%s` for more information",
                        Config.PREFIX + CommRegistry.COMM_TRIGGER_STATUS));
                return false;
            }

            game.setDayLength(this.dayLengthMillis, TimeUnit.MILLISECONDS);

            try {
                game.start(this.channelId, getMode(), inned);
            } catch (final UserFriendlyException e) {
                log.info("Game start aborted due to user friendly exception", e);
                Games.remove(game);
                game.cleanUp();
                throw new UserFriendlyException(e.getMessage(), e);
            } catch (final Exception e) {
                //start failed with a fucked up exception
                Games.remove(game);
                game.cleanUp();
                throw new RuntimeException(String.format("%s, game start aborted due to:%n%s",
                        TextchatUtils.userAsMention(commandCallerId), e.getMessage()), e);
            }
            this.innedUsers.clear();
            return true;
        }
    }

    private TextChannel getThisChannel() {
        final TextChannel tc = Wolfia.getTextChannelById(this.channelId);
        if (tc == null) {
            throw new NullPointerException(String.format("Could not find channel %s of setup", this.channelId));
        }
        return tc;
    }
}
