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

package space.npstr.wolfia.db.entity;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.debug.MaintenanceCommand;
import space.npstr.wolfia.commands.debug.ShutdownCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.IEntity;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.Operation;
import space.npstr.wolfia.utils.TextchatUtils;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by napster on 29.05.17.
 * <p>
 * holds persistent setup information on a channel scope
 */
@Entity
@Table(name = "setups")
public class SetupEntity implements IEntity {

    @Transient
    private static final Logger log = LoggerFactory.getLogger(SetupEntity.class);


    @Id
    @Column(name = "channel_id")
    private long channelId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "inned_users")
    private final Set<Long> innedUsers = new HashSet<>();

    //one of the values of the Games.GAMES enum
    @Column(name = "game")
    private String game = "";

    //optional mode, for example CLASSIC or WILD for Popcorn games
    @Column(name = "mode")
    private String mode = "";

    //day length in milliseconds
    @Column(name = "day_length")
    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10);

    //some basic getters/setters
    @Override
    public void setId(final long id) {
        this.channelId = id;
    }

    @Override
    public long getId() {
        return this.channelId;
    }

    public Games getGame() {
        return Games.valueOf(this.game);
    }

    public void setGame(final Games game) {
        this.game = game.name();
    }

    public GameInfo.GameMode getMode() {
        return GameInfo.GameMode.valueOf(this.mode);
    }

    public void setMode(final GameInfo.GameMode mode) throws IllegalArgumentException {
        if (!Games.getInfo(getGame()).getSupportedModes().contains(mode)) {
            final String message = String.format("Game %s does not support mode %s", getGame().name(), mode);
            throw new IllegalArgumentException(message);
        }
        this.mode = mode.name();
    }

    public long getDayLengthMillis() {
        return this.dayLengthMillis;
    }

    public void setDayLength(final long dayLength, final TimeUnit timeUnit) {
        this.dayLengthMillis = timeUnit.toMillis(dayLength);
    }

    //create a fresh setup; default game is Popcorn, default mode is Wild
    public SetupEntity() {
        this.setGame(Games.POPCORN);
        this.setMode(Games.getInfo(Games.POPCORN).getDefaultMode());
        this.setDayLength(10, TimeUnit.MINUTES);
    }

    public void inUser(final long userId, final Operation success) {
        if (this.innedUsers.contains(userId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s, you have inned already.", TextchatUtils.userAsMention(userId));
        } else {
            this.innedUsers.add(userId);
            success.execute();
            DbWrapper.merge(this);
        }
    }

    public boolean outUser(final long userId) {
        final boolean outted = this.innedUsers.remove(userId);
        DbWrapper.merge(this);
        return outted;
    }

    private void cleanUpInnedPlayers() {
        //did they leave the guild?
        final Set<Long> toBeOuted = new HashSet<>();
        final Guild g = Wolfia.jda.getTextChannelById(this.channelId).getGuild();
        this.innedUsers.forEach(userId -> {
            if (g.getMemberById(userId) == null) {
                toBeOuted.add(userId);
            }
        });
        toBeOuted.forEach(this::outUser);

        //todo whenever time based ins are a thing, this is probably the place to check them
    }

    public void postStatus() {
        Wolfia.handleOutputEmbed(this.channelId, getStatus());
    }

    public MessageEmbed getStatus() {
        //clean up first
        cleanUpInnedPlayers();

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Setup for channel #" + Wolfia.jda.getTextChannelById(this.channelId).getName());
        eb.setDescription(Games.get(this.channelId) == null ? "Game has **NOT** started yet." : "Game has started.");

        //games
        final StringBuilder possibleGames = new StringBuilder();
        Arrays.stream(Games.values()).forEach(g -> possibleGames.append(g == getGame() ? "`[x]` " : "`[ ]` ").append(g.textRep).append("\n"));
        eb.addField("Game", possibleGames.toString(), true);

        //modes
        final StringBuilder possibleModes = new StringBuilder();
        Games.getInfo(getGame()).getSupportedModes().forEach(mode -> possibleModes.append(mode.equals(getMode()) ? "`[x]` " : "`[ ]` ").append(mode).append("\n"));
        eb.addField("Mode", possibleModes.toString(), true);

        //day length
        eb.addField("Day length", TextchatUtils.formatMillis(this.dayLengthMillis), true);

        //accepted player numbers
        eb.addField("Allowed players",
                Games.getInfo(getGame()).getAcceptablePlayerNumbers(getMode()),
                true);

        //inned players
        final String listInned = String.join(", ", this.innedUsers.stream().map(u -> "`" + Wolfia.jda.getTextChannelById(this.channelId).getGuild().getMemberById(u).getEffectiveName() + "`").collect(Collectors.toList()));
        eb.addField("Inned players (**" + this.innedUsers.size() + "**)", listInned, true);
        return eb.build();
    }

    //needs to be synchronized so only one incoming command at a time can be in here
    public synchronized boolean startGame(final long commandCallerId) throws IllegalGameStateException {
        //need to synchronize on a class level due to this being an entity object that may be loaded twice from the database
        synchronized (SetupEntity.class) {
            if (MaintenanceCommand.getMaintenanceFlag() || ShutdownCommand.getShutdownFlag()) {
                Wolfia.handleOutputMessage(this.channelId, "The bot is under maintenance. Please try starting a game later.");
                return false;
            }

            if (!this.innedUsers.contains(commandCallerId)) {
                Wolfia.handleOutputMessage(this.channelId, "%s: Only players that inned can start the game!", TextchatUtils.userAsMention(commandCallerId));
                return false;
            }

            //is there a game running already in this channel?
            if (Games.get(this.channelId) != null) {
                Wolfia.handleOutputMessage(this.channelId,
                        "%s, there is already a game going on in this channel!",
                        TextchatUtils.userAsMention(commandCallerId));
                return false;
            }

            final Game game;
            try {
                game = this.getGame().clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException e) {
                throw new IllegalGameStateException("Internal error, could not create the specified game.", e);
            }

            cleanUpInnedPlayers();
            final Set<Long> inned = Collections.unmodifiableSet(this.innedUsers);
            if (!game.isAcceptablePlayerCount(inned.size(), getMode())) {
                Wolfia.handleOutputMessage(this.channelId,
                        "There aren't enough (or too many) players signed up! Please use `%s%s` for more information",
                        Config.PREFIX, StatusCommand.COMMAND);
                return false;
            }

            game.setDayLength(this.dayLengthMillis, TimeUnit.MILLISECONDS);

            try {
                game.start(this.channelId, getMode(), inned);
            } catch (final Exception e) {
                //start failed
                Games.remove(game);
                game.cleanUp();
                log.warn("Game start aborted due to exception", e);
                Wolfia.handleOutputMessage(this.channelId, "%s, game start aborted due to:\n%s",
                        TextchatUtils.userAsMention(commandCallerId), e.getMessage());
                return false;
            }
            this.innedUsers.clear();
            DbWrapper.merge(this);
            return true;
        }
    }
}
