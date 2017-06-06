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
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.db.DbWrapper;
import space.npstr.wolfia.db.IEntity;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.game.Popcorn;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.Operation;
import space.npstr.wolfia.utils.TextchatUtils;

import javax.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by napster on 29.05.17.
 * <p>
 * holds persistent setup information on a channel scope
 */
@Entity
@Table(name = "setups")
public class SetupEntity implements IEntity {

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

    //some basic getters/setters
    @Override
    public void setId(final long id) {
        this.channelId = id;
    }

    @Override
    public long getId() {
        return this.channelId;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public Games getGame() {
        return Games.valueOf(this.game);
    }

    public void setGame(final Games game) {
        this.game = game.name();
    }

    public String getMode() {
        return this.mode;
    }

    public void setMode(final String mode) throws IllegalArgumentException {
        if (!Game.getGameModes(getGame()).contains(mode)) {
            final String message = String.format("Game %s does not support mode %s", getGame().name(), mode);
            throw new IllegalArgumentException(message);
        }
        this.mode = mode;
    }

    //create a fresh setup; default game is Popcorn, default mode is Wild
    public SetupEntity() {
        this.setGame(Games.POPCORN);
        this.setMode(Popcorn.MODE.WILD.name());
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

    public void outUser(final long userId) {
        this.innedUsers.remove(userId);
        DbWrapper.merge(this);
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

    public void postStats() {
        Wolfia.handleOutputEmbed(this.channelId, getStatus());
    }

    public MessageEmbed getStatus() {
        //clean up first
        cleanUpInnedPlayers();

        final EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Setup for channel #" + Wolfia.jda.getTextChannelById(this.channelId).getName());
        eb.setDescription(Games.get(this.channelId) == null ? "Game has **NOT** started yet." : "Gam has started.");

        //games
        final StringBuilder possibleGames = new StringBuilder();
        Arrays.stream(Games.values()).forEach(g -> possibleGames.append(g == getGame() ? "`[x]` " : "`[ ]` ").append(g.textRep).append("\n"));
        eb.addField("Game", possibleGames.toString(), true);

        //modes
        final StringBuilder possibleModes = new StringBuilder();
        Game.getGameModes(getGame()).forEach(mode -> possibleModes.append(mode.equals(getMode()) ? "`[x]` " : "`[ ]` ").append(mode).append("\n"));
        eb.addField("Mode", possibleModes.toString(), true);

        //accepted player numbers
        eb.addField("Allowed players",
                String.join(", ", Game.getAcceptablePlayerNumbers(getGame()).stream().map(i -> "`" + i + "`").collect(Collectors.toList())),
                true);

        //inned players
        final String listInned = String.join(", ", this.innedUsers.stream().map(u -> "`" + Wolfia.jda.getTextChannelById(this.channelId).getGuild().getMemberById(u).getEffectiveName() + "`").collect(Collectors.toList()));
        eb.addField("Inned players (**" + this.innedUsers.size() + "**)", listInned, true);
        return eb.build();
    }

    public synchronized void startGame(final long commandCallerId) throws IllegalGameStateException {

        if (Wolfia.restartFlag) {
            Wolfia.handleOutputMessage(this.channelId, "The bot is getting ready to restart. Please try playing a game later.");
            return;
        }

        if (!this.innedUsers.contains(commandCallerId)) {
            Wolfia.handleOutputMessage(this.channelId, "%s: Only players that inned can start the game!", TextchatUtils.userAsMention(commandCallerId));
            return;
        }

        //is there a game running already in this channel?
        if (Games.get(this.channelId) != null) {
            Wolfia.handleOutputMessage(this.channelId,
                    "%s, there is already a game going on in this channel!",
                    TextchatUtils.userAsMention(commandCallerId));
            return;
        }

        final Game game;
        try {
            game = this.getGame().clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalGameStateException("Internal error, could not create the specified game.");
        }

        cleanUpInnedPlayers();
        if (!game.isAcceptablePlayerCount(this.innedUsers.size())) {
            Wolfia.handleOutputMessage(this.channelId,
                    "There aren't enough (or too many) players signed up! Please use `%s%s` for more information",
                    Config.PREFIX, StatusCommand.COMMAND);
            return;
        }

        game.setChannelId(this.channelId);
        Games.set(game);
        if (game.start(this.innedUsers)) {
            this.innedUsers.clear();
            DbWrapper.merge(this);
        }
    }
}
