/*
 * Copyright (C) 2016-2023 the original author or authors
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.util.InviteCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.type.OAuth2Scope;
import space.npstr.wolfia.domain.oauth2.OAuth2Service;
import space.npstr.wolfia.domain.room.ManagedPrivateRoom;
import space.npstr.wolfia.domain.room.PrivateRoomQueue;
import space.npstr.wolfia.domain.settings.ChannelSettingsCommand;
import space.npstr.wolfia.domain.setup.StatusCommand;
import space.npstr.wolfia.domain.stats.GameStats;
import space.npstr.wolfia.domain.stats.InsertActionStats;
import space.npstr.wolfia.domain.stats.InsertGameStats;
import space.npstr.wolfia.domain.stats.InsertPlayerStats;
import space.npstr.wolfia.domain.stats.ReplayCommand;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Scope;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.LogTheStackException;

/**
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Game.class);

    protected final GameResources resources;

    //to be used to execute tasks for each game
    //each task scheduled on it needs to check of the game is still running once it continues execution to avoid race
    //conditions in games ending outside of main loop (shots, bombs, forced destroy by bot admin, etc)
    protected final ExceptionLoggingExecutor executor = new ExceptionLoggingExecutor(10,
            r -> new Thread(r, "game-in-channel-" + Game.this.getChannelId() + "-executor-thread"));

    //commonly used fields
    protected long channelId = -1;
    protected long guildId = -1;
    protected final long selfUserId;
    protected GameInfo.GameMode mode;
    protected final List<Player> players = new ArrayList<>();
    protected volatile boolean running = false;
    protected long accessRoleId;
    protected ManagedPrivateRoom wolfChat = null;
    protected final Set<Integer> hasDayEnded = new HashSet<>();

    //stats keeping fields
    protected InsertGameStats insertGameStats = null;
    protected GameStats gameStats = null;
    protected final Map<Long, InsertPlayerStats> playersStats = new HashMap<>();
    protected final AtomicInteger actionOrder = new AtomicInteger();

    protected Game(GameResources gameResources) {
        this.resources = gameResources;
        this.selfUserId = resources.getShardManager().getShards().stream().findAny()
                .map(shard -> shard.getSelfUser().getIdLong())
                .orElseThrow();
    }


    /**
     * @return Returns the main channel where the game is running
     */
    public long getChannelId() {
        return this.channelId;
    }

    public long getGuildId() {
        return this.guildId;
    }

    public long getPrivateRoomGuildId() {
        if (this.wolfChat == null)
            return -1;
        else {
            return this.wolfChat.getGuildId();
        }
    }

    /**
     * @return the role pm of a user
     */
    public String getRolePm(Member member) throws IllegalGameStateException {
        return getPlayer(member.getUser().getIdLong()).getRolePm();
    }

    /**
     * @return true if the user is playing in this game (dead or alive), false if not
     */
    public boolean isUserPlaying(User user) {
        long userId = user.getIdLong();
        return this.players.stream().anyMatch(p -> p.userId == userId);
    }

    public boolean isUserPlaying(Member member) {
        return isUserPlaying(member.getUser());
    }

    /**
     * @param signedUpCount amount of players that have signed up
     * @return true if a game can be started with the provided amount of players
     */
    public boolean isAcceptablePlayerCount(int signedUpCount, GameInfo.GameMode mode) {
        return Games.getInfo(this.getClass()).isAcceptablePlayerCount(signedUpCount, mode);
    }

    /**
     * @return time when the game started
     */
    public long getStartTime() {
        if (this.insertGameStats != null) return this.insertGameStats.getStartTime();
        else return -1;
    }

    /**
     * this is used to keep stats, call this whenever a listener sees the user post something during an ongoing game
     */
    public void userPosted(Message message) {
        if (!this.running) return;

        long userId = message.getAuthor().getIdLong();
        InsertPlayerStats ps = this.playersStats.get(userId);
        if (ps != null) {
            ps.bumpPosts(message.getContentRaw().length());
        }
    }

    /**
     * @return the main game channel
     */
    protected TextChannel fetchGameChannel() {
        return fetchTextChannel(this.channelId);
    }

    /**
     * @return the baddie game channel
     * Might throw an exception if called in the wrong game mode or never return
     */
    protected TextChannel fetchBaddieChannel() {
        return fetchTextChannel(this.wolfChat.getChannelId());
    }

    public boolean isLiving(User user) {
        return isLiving(user.getIdLong());
    }

    public boolean isLiving(long userId) {
        for (Player p : this.players) {
            if (p.userId == userId && p.isAlive()) {
                return true;
            }
        }
        return false;
    }

    protected Player getPlayer(long userId) throws IllegalGameStateException {
        for (Player p : this.players) {
            if (p.userId == userId) {
                return p;
            }
        }
        throw new IllegalGameStateException("Requested player " + userId + " is not in the player list");
    }

    public Player getPlayer(User user) throws IllegalGameStateException {
        return getPlayer(user.getIdLong());
    }

    protected Set<Player> getVillagers() {
        return this.players.stream()
                .filter(Player::isGoodie)
                .collect(Collectors.toSet());
    }

    protected List<Player> getLivingVillage() {
        return this.players.stream()
                .filter(Player::isAlive)
                .filter(Player::isGoodie)
                .collect(Collectors.toList());
    }

    protected Set<Long> getLivingVillageIds() {
        return this.players.stream()
                .filter(Player::isAlive)
                .filter(Player::isGoodie)
                .map(Player::getUserId)
                .collect(Collectors.toSet());
    }

    protected Set<Player> getWolves() {
        return this.players.stream()
                .filter(Player::isBaddie)
                .collect(Collectors.toSet());
    }

    protected Set<Long> getWolvesIds() {
        return this.players.stream()
                .filter(Player::isBaddie)
                .map(Player::getUserId)
                .collect(Collectors.toSet());
    }

    protected List<Player> getLivingWolves() {
        return this.players.stream()
                .filter(Player::isAlive)
                .filter(Player::isBaddie)
                .collect(Collectors.toList());
    }

    protected Set<String> getLivingWolvesMentions() {
        return this.players.stream()
                .filter(Player::isAlive)
                .filter(Player::isBaddie)
                .map(p -> TextchatUtils.userAsMention(p.userId))
                .collect(Collectors.toSet());
    }

    protected List<Player> getLivingPlayers() {
        return this.players.stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());
    }

    protected Set<Long> getLivingPlayerIds() {
        return this.players.stream()
                .filter(Player::isAlive)
                .map(Player::getUserId)
                .collect(Collectors.toSet());
    }

    protected List<String> getLivingPlayerMentions() {
        return this.players.stream()
                .filter(Player::isAlive)
                .map(p -> TextchatUtils.userAsMention(p.userId))
                .collect(Collectors.toList());
    }

    public boolean isLivingWolf(Member m) {
        return getLivingWolves().stream().anyMatch(player -> player.userId == m.getUser().getIdLong());
    }

    //do not post this before the game is over
    //set wwFlair to true to have werewolf flaor, otherwise mafia flair will be used
    protected String listTeams(boolean... wwFlair) {
        if (this.running) {
            log.warn("listTeams() called in a running game");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Village: ");
        getVillagers().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        if (wwFlair.length > 0 && wwFlair[0]) {
            sb.append("\nWolves: ");
        } else {
            sb.append("\nMafia: ");
        }
        getWolves().forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    protected String listLivingPlayers() {
        List<Player> living = getLivingPlayers();
        StringBuilder sb = new StringBuilder("Living players (**").append(living.size()).append("**) :");
        living.forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    /**
     * Calls this from your start() implementation
     * <p>
     * Does general checks of the arguments provided to start()
     *
     * @param channelId
     *         main channel where the game shall run
     * @param mode
     *         the chosen game mode
     * @param innedPlayers
     *         the players who signed up
     *
     * @throws IllegalArgumentException
     *         if any of the provided arguments fail the checks
     */
    protected void doArgumentChecksAndSet(long channelId, GameInfo.GameMode mode, Set<Long> innedPlayers) {
        if (this.running) {
            throw new IllegalStateException("Cannot start a game that is running already");
        }
        TextChannel channel = resources.getShardManager().getTextChannelById(channelId);
        if (channelId <= 0 || channel == null) {
            throw new IllegalArgumentException(String.format(
                    "Cannot start a game with invalid/no channel (channelId: %s) set.", channelId)
            );
        }
        this.channelId = channelId;
        this.guildId = channel.getGuild().getIdLong();
        if (!Games.getInfo(this).getSupportedModes().contains(mode)) {
            throw new IllegalArgumentException(String.format(
                    "Mode %s not supported by game %s", mode.name(), Games.POPCORN.name())
            );
        }
        this.mode = mode;

        if (!Games.getInfo(this).isAcceptablePlayerCount(innedPlayers.size(), mode)) {
            throw new IllegalArgumentException(String.format("There aren't enough (or too many) players signed up! " +
                    "Please use `%s` for more information", WolfiaConfig.DEFAULT_PREFIX + StatusCommand.TRIGGER));
        }
    }

    /**
     * Calls this from your start() implementation after having checked and set the arguments
     * <p>
     * Checks whether all required permissions for running the chosen game and mode are available to the bot
     * <p>
     * Prepares the channel for moderated games.
     *
     * @param moderated
     *         moderated games require additional permissions
     *
     * @throws UserFriendlyException
     *         if the bot is missing permissions to run the game in the channel
     */
    protected void doPermissionCheckAndPrepareChannel(boolean moderated) {
        TextChannel gameChannel = fetchGameChannel();
        Guild g = gameChannel.getGuild();

        //check permissions
        Set<Permission> toAcquireInChannelScope = new HashSet<>();
        Games.getInfo(this).getRequiredPermissions(this.mode).forEach((permission, scope) -> {

            if (scope == Scope.CHANNEL) {
                toAcquireInChannelScope.add(permission);
            }
            //lets not worry about things we arent even using currently and possibly never will
        });
        if (!toAcquireInChannelScope.isEmpty()) {
            //is the bot allowed to manage permissions for this channel?
            if (!g.getSelfMember().hasPermission(gameChannel, Permission.MANAGE_PERMISSIONS)) {
                throw new UserFriendlyException(String.format(
                        "To run a %s game in %s mode in this channel, I need the permission to `%s` in this channel",
                        Games.POPCORN.textRep, this.mode.name(), Permission.MANAGE_PERMISSIONS)
                );
            }
            RoleAndPermissionUtils.acquireChannelPermissions(gameChannel, toAcquireInChannelScope.toArray(new Permission[0]));
        }

        if (moderated) {
            //is this a non-public channel, and if yes, has an existing access role been set?
            boolean isChannelPublic = g.getPublicRole()
                    .hasPermission(gameChannel, Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL);
            if (isChannelPublic) {
                this.accessRoleId = g.getIdLong(); //public role / @everyone, guaranteed to exist
            } else {
                this.accessRoleId = resources.getChannelSettingsService()
                        .channel(this.channelId).getOrDefault().getAccessRoleId().orElse(0L);
                Role accessRole = g.getRoleById(this.accessRoleId);
                if (accessRole == null) {
                    throw new UserFriendlyException(String.format(
                            "Non-public channel has been detected (`@everyone` is missing `%s` and/or `%s` permissions)." +
                                    " The chosen game and mode requires the channel to be either public, or have an access role set up." +
                                    " Talk to an Admin/Moderator of your server to fix this or set the access role up with `%s`." +
                                    " Please refer to the documentation under %s",
                            Permission.MESSAGE_SEND.getName(), Permission.VIEW_CHANNEL.getName(),
                            WolfiaConfig.DEFAULT_PREFIX + ChannelSettingsCommand.TRIGGER, App.DOCS_LINK
                    ));
                }
                if (!accessRole.hasPermission(gameChannel, Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL)) {
                    throw new UserFriendlyException(String.format(
                            "The configured access role `%s` is missing `%s` and/or `%s` permissions in this channel." +
                                    " Talk to an admin of your server to fix this." +
                                    " Please refer to the documentation under %s",
                            accessRole.getName(), Permission.MESSAGE_SEND.getName(),
                            Permission.VIEW_CHANNEL.getName(), App.DOCS_LINK
                    ));
                }
            }

            //is the bot allowed to manage permissions for this channel?
            if (!g.getSelfMember().hasPermission(gameChannel, Permission.MANAGE_PERMISSIONS)) {
                throw new UserFriendlyException(String.format(
                        "To run a %s game in %s mode in this channel, I need the permission to `%s` in this channel",
                        Games.POPCORN.textRep, this.mode.name(), Permission.MANAGE_PERMISSIONS)
                );
            }


            try {
                prepareChannel();
            } catch (PermissionException e) {
                log.error("Could not prepare channel {}, id: {}, due to missing permission: {}", gameChannel.getName(),
                        gameChannel.getId(), e.getPermission().getName(), e);
                throw new UserFriendlyException(String.format(
                        "The bot is missing the permission `%s` to run the selected game and mode in this channel.",
                        e.getPermission().getName()
                ));
            }
        }
    }

    /**
     * Obtains a character setup and rands the roles and alignments between the inned players
     *
     * @param innedPlayers players that have inned
     */
    protected void randCharacters(Set<Long> innedPlayers) {
        // - rand the characters
        CharakterSetup charakterSetup = Games.getInfo(this).getCharacterSetup(this.mode, innedPlayers.size());
        List<Long> rand = new ArrayList<>(innedPlayers);
        Collections.shuffle(rand);

        if (charakterSetup.size() != innedPlayers.size()) {
            throw new IllegalArgumentException(String.format(
                    "The received character setup (%s) has a different size than the inned players (%s)",
                    charakterSetup.size(), innedPlayers.size())
            );
        }

        this.players.clear();
        int i = 0;
        for (Charakter c : charakterSetup.getRandedCharakters()) {
            long randedUserId = rand.get(i);
            this.players.add(new Player(resources, randedUserId, this.channelId, this.guildId, c.alignment, c.role, i + 1));
            i++;
        }
    }

    protected ManagedPrivateRoom allocatePrivateRoom() {
        PrivateRoomQueue privateRoomQueue = resources.getPrivateRoomQueue();
        return privateRoomQueue.poll()
                .orElseGet(() -> {
                    RestActions.sendMessage(fetchGameChannel(),
                            "Acquiring a private server for the wolves...this may take a while.");
                    log.error("Ran out of free private guilds. Please add moar.");
                    try { //oh yeah...we are waiting till infinity if necessary
                        return privateRoomQueue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting for a private server.");
                        throw new UserFriendlyException("Could not allocate a private server.");
                    }
                });
    }

    /**
     * Prepares the channel for a moderated game
     *
     * @throws PermissionException
     *         if the bot is missing permissions to edit permission overrides for members and roles
     */
    protected void prepareChannel() {
        TextChannel gameChannel = fetchGameChannel();
        Guild g = gameChannel.getGuild();

        // - ensure write access for the bot in the game channel
        // this can be done with complete() as most of the time (after the first game) it will already be in place
        // and will prevent messages getting lost due to queue() sometimes taking a while
        RoleAndPermissionUtils.grant(gameChannel, g.getSelfMember(), Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION).complete();

        // - no writing access and reaction adding for @everyone/access role in the game channel during the game
        RoleAndPermissionUtils.deny(gameChannel, g.getRoleById(this.accessRoleId),
                Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION).queue(null, RestActions.defaultOnFail());
    }

    /**
     * this should revert each and everything the game touches in terms of discord roles and permissions to normal
     * most likely this includes deleting all discord roles used in the game and resetting player's and the access
     * role's permission overrides for the game channel
     *
     * @param complete optionally set to true to complete these operations before returning
     */
    //revert whatever prepareChannel() did in reverse order
    public void resetRolesAndPermissions(boolean... complete) {

        TextChannel channel = resources.getShardManager().getTextChannelById(this.channelId);
        if (channel == null) {
            //we probably left the guild
            log.warn("Could not find channel {} to reset roles and permissions in there", this.channelId);
            return;
        }
        Guild g = channel.getGuild();
        List<Permission> missingPermissions = new ArrayList<>();
        List<Future<?>> toComplete = new ArrayList<>();

        //reset permission override for the players
        try {
            for (Player player : this.players) {
                toComplete.add(RoleAndPermissionUtils.clear(channel, g.getMemberById(player.userId),
                        Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION).submit());
            }
        } catch (PermissionException e) {
            missingPermissions.add(e.getPermission());
        }

        //reset permission override for the access role in the game channel
        try {
            Role accessRole = g.getRoleById(this.accessRoleId);
            if (accessRole != null) {
                //todo don't grant MESSAGE_ADD_REACTION if it wasn't granted. we need both things happening in a single RestAction to work properly
                toComplete.add(RoleAndPermissionUtils.grant(channel, accessRole, Permission.MESSAGE_SEND, Permission.MESSAGE_ADD_REACTION).submit());
            }
        } catch (PermissionException e) {
            missingPermissions.add(e.getPermission());
        }

        if (!missingPermissions.isEmpty()) {
            RestActions.sendMessage(channel,
                    String.format("Tried to clean up channel, but was missing the following permissions: `%s`",
                            missingPermissions.stream()
                                    .map(Permission::getName)
                                    .distinct()
                                    .collect(Collectors.joining("`, `"))));
        }

        if (complete.length > 0 && complete[0]) {
            for (Future<?> f : toComplete) {
                try {
                    f.get();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {
                    // ignored
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
            try {
                this.wolfChat.endUsage();
            } catch (IllegalStateException ignored) {
                //dont really care about this one, its fine if usage has been stopped already
            }
        }
        resetRolesAndPermissions(true);
        this.executor.shutdown(); //dont use shutdownNow() as that might kill the thread executing this line of code
    }

    //public for eval usage
    public void destroy(Throwable reason) {
        String reasonMessage = "No reason provided";
        if (reason != null) reasonMessage = reason.getMessage();
        String logMessage = String.format("Game in channel %s destroyed due to %s", this.channelId, reasonMessage);
        if (reason instanceof UserFriendlyException) {
            log.info(logMessage, reason);
        } else {
            log.error(logMessage, reason);
        }
        cleanUp();
        resources.getGameRegistry().remove(this);
        TextChannel channel = resources.getShardManager().getTextChannelById(this.channelId);
        if (channel != null) {
            RestActions.sendMessage(channel,
                    String.format("Game has been stopped due to:"
                                    + "\n`%s`"
                                    + "\nSorry about that. The issue has been logged and will hopefully be fixed soon."
                                    + "\nIf you want to help solve this as fast as possible, please join our support guild."
                                    + "\nSay `%s` to receive an invite.",
                            reasonMessage, WolfiaConfig.DEFAULT_PREFIX + InviteCommand.TRIGGER));
        }
    }

    protected boolean isOnlyVillageLeft() {
        return getLivingPlayers().stream().allMatch(Player::isGoodie);
    }

    //this check will probably get much more sophisticated with more complicated roles
    protected boolean isParityReached() {
        return getLivingWolves().size() >= getLivingVillage().size();
    }


    /**
     * Checks whether any win conditions have been met, and reveals the game if yes
     */
    protected boolean isGameOver(boolean... wwFlair) {
        boolean gameEnding = false;
        boolean villageWins = true;
        String out = "";
        if (isOnlyVillageLeft()) {
            gameEnding = true;
            this.running = false;
            if (wwFlair.length > 0 && wwFlair[0]) {
                out = "All wolves dead! **Village wins.** Thanks for playing!\nTeams:\n" + listTeams(true);
            } else { //Mafia flair
                out = "All mafia dead! **Town wins.** Thanks for playing!\nTeams:\n" + listTeams();
            }
        }
        if (isParityReached()) {
            gameEnding = true;
            this.running = false;
            villageWins = false;
            if (wwFlair.length > 0 && wwFlair[0]) {
                out = "Parity reached! **Wolves win.** Thanks for playing.\nTeams:\n" + listTeams(true);
            } else { //Mafia Flair
                out = "Parity reached! **Mafia wins.** Thanks for playing.\nTeams:\n" + listTeams();
            }
        }

        if (gameEnding) {
            this.insertGameStats.addAction(simpleAction(this.selfUserId, Actions.GAMEEND, -1));
            this.insertGameStats.setEndTime(System.currentTimeMillis());

            if (villageWins) {
                this.insertGameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.VILLAGE)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            } else {
                //woofs win
                this.insertGameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.WOLF)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            }
            resources.getMetricsService().gamesPlayed(
                            this.insertGameStats.getGameType(),
                            this.insertGameStats.getGameMode()
                    )
                    .increment();
            try {
                this.gameStats = resources.getStatsService().recordGameStats(this.insertGameStats);

                long gameId = this.gameStats.getGameId();
                out += String.format("%nThis game's id is **%s**, you can watch its replay with `%s %s`",
                        gameId, WolfiaConfig.DEFAULT_PREFIX + ReplayCommand.TRIGGER, gameId);
            } catch (Exception e) {
                log.error("Db blew up saving game stats", e);
                out += "The database it not available currently, a replay of this game will not be available.";
            }
            cleanUp();
            TextChannel gameChannel = fetchGameChannel();
            String info = Games.getInfo(this).textRep();
            long gameId = this.gameStats.getGameId();
            log.info("Game #{} ended in guild {} {}, channel #{} {}, {} {} {} players",
                    gameId, gameChannel.getGuild().getName(), gameChannel.getGuild().getIdLong(),
                    gameChannel.getName(), gameChannel.getIdLong(), info, this.mode.textRep, this.players.size());
            // removing the game from the registry has to be the very last statement, since if a restart is queued, it
            // waits for an empty games registry
            RestActions.sendMessage(fetchGameChannel(), out,
                    ignoredMessage -> resources.getGameRegistry().remove(this),
                    throwable -> {
                        log.error("Failed to send last message of game #{}", gameId, throwable);
                        resources.getGameRegistry().remove(this);
                    });
            return true;
        }

        return false;
    }

    protected EmbedBuilder listLivingPlayersWithNumbers(Player... except) {
        NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
        TextChannel gameChannel = fetchGameChannel();
        Guild g = gameChannel.getGuild();
        neb.setTitle("Living players");
        neb.setDescription("Game: " + Games.getInfo(this).textRep() + " " + this.mode.textRep + " on " + g.getName() + " in #" + gameChannel.getName());

        NiceEmbedBuilder.ChunkingField list = new NiceEmbedBuilder.ChunkingField("", true);
        Set<Player> dontAdd = new HashSet<>(Arrays.asList(except));
        for (Player p : getLivingPlayers()) {
            if (!dontAdd.contains(p)) {
                list.add(p.numberAsEmojis() + " " + p.bothNamesFormatted(), true);
            }
        }
        neb.addField(list);
        return neb;
    }

    protected void addToBaddieGuild(Player player) {
        OAuth2Service oAuth2Service = resources.getoAuth2Service();

        String accessToken = oAuth2Service.getAccessTokenForScope(player.getUserId(), OAuth2Scope.GUILD_JOIN);
        if (accessToken != null) {
            fetchBaddieChannel().getGuild().addMember(accessToken, UserSnowflake.fromId(player.getUserId())).queue();
        }
        // TODO tell player if they have no valid token
    }

    protected Future<?> scheduleIfGameStillRuns(Runnable runnable, Duration delay) {
        return this.executor.schedule(() -> {
            if (running) {
                runnable.run();
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    //an way to create ActionStats object with a bunch of default/automatically generated values, like time stamps
    protected abstract InsertActionStats simpleAction(long actor, Actions action, long target);

    /**
     * Sets the day length
     *
     * @param dayLength
     *         desired length of the day
     */
    public abstract void setDayLength(Duration dayLength);

    /**
     * @return a status of the game
     */
    public abstract EmbedBuilder getStatus();

    /**
     * Start a game
     * <p>
     * IMPORTANT: In 99.483% of cases this needs to be implemented as synchronized.
     * <p>
     * Things this needs to take care of include:
     * - setting the channelId, game mode and players
     * - creating, sending and saving the role pms
     *
     * @param channelId
     *         main channel where the game shall run
     * @param mode
     *         the chosen game mode
     * @param innedPlayers
     *         the players who signed up
     */
    public abstract void start(long channelId, GameInfo.GameMode mode, Set<Long> innedPlayers);

    /**
     * Let the game handle a command a user issued
     *
     * @param context
     *         the context of the issued command
     *
     * @return true if the command was executed successful
     *
     * @throws IllegalGameStateException
     *         if the command entered led to an illegal game state
     */
    public abstract boolean issueCommand(CommandContext context)
            throws IllegalGameStateException;


    //this method assumes that the id itself is legit and not a mistake
    // it is an attempt to improve the occasional inconsistency of discord which makes looking up entities a gamble
    // the main feature being the non-null return contract, over the @Nullable contract of looking the entity up in JDA
    private TextChannel fetchTextChannel(long channelId) {
        ShardManager shardManager = resources.getShardManager();
        TextChannel tc = shardManager.getTextChannelById(channelId);
        int attempts = 0;
        while (tc == null) {
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Failed to fetch channel #" + channelId);
            }
            log.error("Could not find channel {}, retrying in a moment", channelId, new LogTheStackException());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tc = shardManager.getTextChannelById(channelId);
        }
        return tc;
    }
}
