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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.IGameCommand;
import space.npstr.wolfia.db.entity.PrivateGuild;
import space.npstr.wolfia.db.entity.stats.GameStats;
import space.npstr.wolfia.db.entity.stats.PlayerStats;
import space.npstr.wolfia.utils.IllegalGameStateException;
import space.npstr.wolfia.utils.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.TextchatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by npstr on 14.09.2016
 * <p>
 * Provides some common methods for all games, like keeping players and queries about the players
 * <p>
 * creating these should be lightweight and not cause any permanent "damage"
 * they may be discarded a few times before one actually starts
 * a created game that hasn't started can answer questions about the modes it supports, supported player counts, etc
 * <p>
 * on contrast, starting a game is serious business
 * it needs to receive a unique number (preferably increasing)
 * it will have to cause outputs in the main game channel and in private channels for role pms
 * all this means a started game has to be treated carefully, both for data consistency and to keep salt levels due to
 * technical problems at bay
 */
public abstract class Game {

    private static final Logger log = LoggerFactory.getLogger(Game.class);

    //to be used to execute tasks for each game
    protected final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
            r -> new Thread(r, "game-in-channel-" + Game.this.channelId + "-helper-thread"));

    //commonly used fields
    protected long channelId = -1;
    protected final Map<Long, String> rolePMs = new HashMap<>();
    protected GameInfo.GameMode mode;
    protected final Set<Player> players = new HashSet<>();
    protected volatile boolean running = false;
    protected long accessRoleId;
    protected PrivateGuild wolfChat = null;

    //stats keeping fields
    protected GameStats gameStats = null;
    protected final Map<Long, PlayerStats> playersStats = new HashMap<>();
    protected final AtomicInteger actionOrder = new AtomicInteger();


    /**
     * @return Returns the main channel where the game is running
     */
    public long getChannelId() {
        return this.channelId;
    }

    /**
     * @return the role pm of a user
     */
    public String getRolePm(final long userId) {
        return this.rolePMs.get(userId);
    }

    /**
     * @return true if the user is playing in this game (dead or alive), false if not
     */
    public boolean isUserPlaying(final long userId) {
        return this.players.stream().anyMatch(p -> p.userId == userId);
    }

    /**
     * @param signedUpCount amount of players that have signed up
     * @return true if a game can be started with the provided amount of players
     */
    public boolean isAcceptablePlayerCount(final int signedUpCount, final GameInfo.GameMode mode) {
        return Games.getInfo(this.getClass()).getAcceptablePlayerNumbers(mode).contains(signedUpCount);
    }

    /**
     * this is used to keep stats, call this whenever a listener sees the user post something during an ongoing game
     */
    public void userPosted(final Message message) {
        if (!this.running) return;

        final long userId = message.getAuthor().getIdLong();
        final PlayerStats ps = this.playersStats.get(userId);
        if (ps != null) {
            ps.bumpPosts(message.getRawContent().length());
        }
    }

    protected boolean isLiving(final long userId) {
        for (final Player p : this.players) {
            if (p.userId == userId && p.isLiving()) {
                return true;
            }
        }
        return false;
    }

    protected Player getPlayer(final long userId) throws IllegalGameStateException {
        for (final Player p : this.players) {
            if (p.userId == userId) {
                return p;
            }
        }
        throw new IllegalGameStateException("Requested player " + userId + " is not in the player list");
    }

    protected Set<Player> getVillagers() {
        return this.players.stream()
                .filter(player -> !player.isWolf)
                .collect(Collectors.toSet());
    }

    protected Set<Player> getLivingVillage() {
        return this.players.stream()
                .filter(Player::isLiving)
                .filter(player -> !player.isWolf)
                .collect(Collectors.toSet());
    }

    protected Set<Player> getWolves() {
        return this.players.stream()
                .filter(player -> player.isWolf)
                .collect(Collectors.toSet());
    }

    protected Set<Player> getLivingWolves() {
        return this.players.stream()
                .filter(Player::isLiving)
                .filter(player -> player.isWolf)
                .collect(Collectors.toSet());
    }

    protected Set<Player> getLivingPlayers() {
        return this.players.stream()
                .filter(Player::isLiving)
                .collect(Collectors.toSet());
    }

    public boolean isLivingWolf(final Member m) {
        return getLivingWolves().stream().anyMatch(player -> player.userId == m.getUser().getIdLong());
    }

    //do not post this before the game is over
    protected String listTeams() {
        if (this.running) {
            log.warn("listTeams() called in a running game");
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("Village: ");
        getVillagers().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        sb.append("\nWolves: ");
        getWolves().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    protected String listLivingPlayers() {
        final Set<Player> living = getLivingPlayers();
        final StringBuilder sb = new StringBuilder("Living players (**").append(living.size()).append("**) :");
        living.forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    /**
     * Prepares the channel for a moderated game
     *
     * @param players ids of the players that have inned
     * @throws PermissionException if the bot is missing permissions to edit permission overrides for members and roles
     */
    protected void prepareChannel(final Set<Long> players) throws PermissionException {
        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        final Guild g = channel.getGuild();

        // - ensure write access for the bot in the game channel
        // this can be done with complete() as most of the time (after the first game) it will already be in place
        // and will prevent messages getting lost due to queue() sometimes taking a while
        RoleAndPermissionUtils.grant(channel, g.getSelfMember(), Permission.MESSAGE_WRITE).complete();

        // - no writing access and reaction adding for @everyone/access role in the game channel during the game
        RoleAndPermissionUtils.deny(channel, g.getRoleById(this.accessRoleId),
                Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
    }

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting player's and the access
     * role's permission overrides for the game channel
     *
     * @param complete optionally set to true to complete these operations before returning
     */
    @SuppressWarnings("unchecked")
    //revert whatever prepareChannel() did in reverse order
    public void resetRolesAndPermissions(final boolean... complete) {

        final TextChannel channel = Wolfia.jda.getTextChannelById(this.channelId);
        final Guild g = channel.getGuild();
        final List<Permission> missingPermissions = new ArrayList<>();
        final List<Future> toComplete = new ArrayList<>();

        //reset permission override for the players
        try {
            for (final Player player : this.players) {
                toComplete.add(RoleAndPermissionUtils.clear(channel, g.getMemberById(player.userId),
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).submit());
            }
        } catch (final PermissionException e) {
            missingPermissions.add(e.getPermission());
        }

        //reset permission override for the access role in the game channel
        try {
            final Role accessRole = g.getRoleById(this.accessRoleId);
            if (accessRole != null)
                toComplete.add(RoleAndPermissionUtils.clear(channel, accessRole,
                        Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).submit());
        } catch (final PermissionException e) {
            missingPermissions.add(e.getPermission());
        }

        if (missingPermissions.size() > 0) {
            Wolfia.handleOutputMessage(channel,
                    "Tried to clean up channel, but was missing the following permissions: `%s`",
                    String.join("`, `",
                            missingPermissions.stream().map(Permission::getName).distinct().collect(Collectors.toList())
                    ));
        }

        if (complete.length > 0 && complete[0]) {
            for (final Future f : toComplete) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
        }
    }

    /**
     * clean up a running game
     * aka reset any possible permissions and overrides, stop any running threads etc
     */
    public void cleanUp() {
        if (this.wolfChat != null) {
            this.wolfChat.endUsage();
        }
        this.executor.shutdownNow();
        resetRolesAndPermissions();
    }

    /**
     * Sets the day length
     *
     * @param dayLength desired length of the day
     * @param timeUnit  time unit of the provided length
     */
    public abstract void setDayLength(long dayLength, TimeUnit timeUnit);

    /**
     * @return a status of the game
     */
    public abstract String getStatus();

    /**
     * Start a game
     * <p>
     * Things this needs to take care of include:
     * - setting the channelId, game mode and players
     * - creating, sending and saving the role pms
     *
     * @param channelId main channel where the game shall runs
     * @param mode      the chosen game mode
     * @param players   the players who signed up
     */
    public abstract void start(final long channelId, final GameInfo.GameMode mode, Set<Long> players);

    /**
     * Let the game handle a command a user issued
     *
     * @param command     the issued command
     * @param commandInfo the context of the issued command
     * @return true if the command was executed successful
     * @throws IllegalGameStateException if the command entered led to an illegal game state
     */
    public abstract boolean issueCommand(IGameCommand command, CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException;
}
