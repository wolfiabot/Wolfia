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

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlstack.DatabaseException;
import space.npstr.wolfia.App;
import space.npstr.wolfia.Config;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.CommandParser;
import space.npstr.wolfia.commands.GameCommand;
import space.npstr.wolfia.commands.game.StatusCommand;
import space.npstr.wolfia.commands.util.ChannelSettingsCommand;
import space.npstr.wolfia.commands.util.HelpCommand;
import space.npstr.wolfia.commands.util.ReplayCommand;
import space.npstr.wolfia.db.entities.ChannelSettings;
import space.npstr.wolfia.db.entities.PrivateGuild;
import space.npstr.wolfia.db.entities.stats.ActionStats;
import space.npstr.wolfia.db.entities.stats.GameStats;
import space.npstr.wolfia.db.entities.stats.PlayerStats;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Scope;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

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

import static space.npstr.wolfia.commands.CommandHandler.mainTrigger;

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
    protected final ExceptionLoggingExecutor executor = new ExceptionLoggingExecutor(10,
            r -> new Thread(r, "game-in-channel-" + Game.this.getChannelId() + "-executor-thread"));

    //commonly used fields
    protected long channelId = -1;
    protected long guildId = -1;
    protected final Map<Long, String> rolePMs = new HashMap<>();
    protected GameInfo.GameMode mode;
    protected final List<Player> players = new ArrayList<>();
    protected volatile boolean running = false;
    protected long accessRoleId;
    protected PrivateGuild wolfChat = null;
    protected final Set<Integer> hasDayEnded = new HashSet<>();

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

    public long getGuildId() {
        return this.guildId;
    }

    public long getPrivateGuildId() {
        if (this.wolfChat == null)
            return -1;
        else {
            return this.wolfChat.getId();
        }
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
        return Games.getInfo(this.getClass()).isAcceptablePlayerCount(signedUpCount, mode);
    }

    /**
     * @return time when the game started
     */
    public long getStartTime() {
        if (this.gameStats != null) return this.gameStats.getStartTime();
        else return -1;
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

    protected TextChannel getThisChannel() {
        final TextChannel tc = Wolfia.getTextChannelById(this.channelId);
        if (tc == null) {
            throw new NullPointerException(String.format("Could not find channel %s of game", this.channelId));
        }
        return tc;
    }

    protected boolean isLiving(final Member member) {
        return isLiving(member.getUser().getIdLong());
    }

    protected boolean isLiving(final long userId) {
        for (final Player p : this.players) {
            if (p.userId == userId && p.isAlive()) {
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

    protected Player getPlayerByNumber(final int number) throws IllegalGameStateException {
        for (final Player p : this.players) {
            if (p.number == number) {
                return p;
            }
        }
        throw new IllegalGameStateException("Requested player number " + number + " is not in the player list");
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

    public boolean isLivingWolf(final Member m) {
        return getLivingWolves().stream().anyMatch(player -> player.userId == m.getUser().getIdLong());
    }

    //do not post this before the game is over
    //set wwFlair to true to have werewolf flaor, otherwise mafia flair will be used
    protected String listTeams(final boolean... wwFlair) {
        if (this.running) {
            log.warn("listTeams() called in a running game");
        }
        final StringBuilder sb = new StringBuilder();
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
        final List<Player> living = getLivingPlayers();
        final StringBuilder sb = new StringBuilder("Living players (**").append(living.size()).append("**) :");
        living.forEach(p -> sb.append(TextchatUtils.userAsMention(p.userId)).append(" "));
        return sb.toString();
    }

    /**
     * Calls this from your start() implementation
     * <p>
     * Does general checks of the arguments provided to start()
     *
     * @param channelId    main channel where the game shall run
     * @param mode         the chosen game mode
     * @param innedPlayers the players who signed up
     * @throws IllegalArgumentException if any of the provided arguments fail the checks
     */
    protected void doArgumentChecksAndSet(final long channelId, final GameInfo.GameMode mode, final Set<Long> innedPlayers)
            throws IllegalArgumentException {
        if (this.running) {
            throw new IllegalStateException("Cannot start a game that is running already");
        }
        final TextChannel channel = Wolfia.getTextChannelById(channelId);
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
                    "Please use `%s%s` for more information", Config.PREFIX, mainTrigger(StatusCommand.class)));
        }
    }

    /**
     * Calls this from your start() implementation after having checked and set the arguments
     * <p>
     * Checks whether all required permissions for running the chosen game and mode are available to the bot
     * <p>
     * Prepares the channel for moderated games.
     *
     * @param moderated moderated games require additional permissions
     * @throws UserFriendlyException if the bot is missing permissions to run the game in the channel
     */
    protected void doPermissionCheckAndPrepareChannel(final boolean moderated) throws UserFriendlyException, DatabaseException {
        final TextChannel channel = getThisChannel();
        final Guild g = channel.getGuild();

        //check permissions
        final Set<Permission> toAcquireInChannelScope = new HashSet<>();
        Games.getInfo(this).getRequiredPermissions(this.mode).forEach((permission, scope) -> {

            if (scope == Scope.CHANNEL) {
                toAcquireInChannelScope.add(permission);
            } else if (scope == Scope.GUILD) {
                //todo lets not worry about things we arent even using currently and possibly never will
            } else {
                //todo
            }
        });
        RoleAndPermissionUtils.acquireChannelPermissions(channel, toAcquireInChannelScope.toArray(new Permission[toAcquireInChannelScope.size()]));

        if (moderated) {
            //is this a non-public channel, and if yes, has an existing access role been set?
            final boolean isChannelPublic = g.getPublicRole()
                    .hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ);
            if (isChannelPublic) {
                this.accessRoleId = g.getIdLong(); //public role / @everyone, guaranteed to exist
            } else {
                this.accessRoleId = Wolfia.getInstance().dbWrapper.getOrCreate(this.channelId, ChannelSettings.class).getAccessRoleId();
                final Role accessRole = g.getRoleById(this.accessRoleId);
                if (accessRole == null) {
                    throw new UserFriendlyException(String.format(
                            "Non-public channel has been detected (`@everyone` is missing `%s` and/or `%s` permissions)." +
                                    " The chosen game and mode requires the channel to be either public, or have an access role set up." +
                                    " Talk to an Admin/Moderator of your server to fix this or set the access role up with `%s`." +
                                    " Please refer to the documentation under %s",
                            Permission.MESSAGE_WRITE.getName(), Permission.MESSAGE_READ.getName(),
                            Config.PREFIX + mainTrigger(ChannelSettingsCommand.class), App.DOCS_LINK
                    ));
                }
                if (!accessRole.hasPermission(channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                    throw new UserFriendlyException(String.format(
                            "The configured access role `%s` is missing `%s` and/or `%s` permissions in this channel." +
                                    " Talk to an admin of your server to fix this." +
                                    " Please refer to the documentation under %s",
                            accessRole.getName(), Permission.MESSAGE_WRITE.getName(),
                            Permission.MESSAGE_READ.getName(), App.DOCS_LINK
                    ));
                }
            }

            //is the bot allowed to manage permissions for this channel?
            if (!g.getSelfMember().hasPermission(channel, Permission.MANAGE_PERMISSIONS)) {
                throw new UserFriendlyException(String.format(
                        "To run a %s game in %s mode in this channel, I need the permission to `%s` in this channel",
                        Games.POPCORN.textRep, this.mode.name(), Permission.MANAGE_PERMISSIONS)
                );
            }


            try {
                prepareChannel();
            } catch (final PermissionException e) {
                log.error("Could not prepare channel {}, id: {}, due to missing permission: {}", channel.getName(),
                        channel.getId(), e.getPermission().getName(), e);
                throw new UserFriendlyException(String.format(
                        "The bot is missing the permission `%s` to run the selected game and mode in this channel.",
                        e.getPermission().getName()
                ), e);
            }
        }
    }

    /**
     * Obtains a character setup and rands the roles and alignments between the inned players
     *
     * @param innedPlayers players that have inned
     */
    protected void randCharacters(final Set<Long> innedPlayers) {
        // - rand the characters
        final CharakterSetup charakterSetup = Games.getInfo(this).getCharacterSetup(this.mode, innedPlayers.size());
        final List<Long> rand = new ArrayList<>(innedPlayers);
        Collections.shuffle(rand);

        if (charakterSetup.size() != innedPlayers.size()) {
            throw new IllegalArgumentException(String.format(
                    "The received character setup (%s) has a different size than the inned players (%s)",
                    charakterSetup.size(), innedPlayers.size())
            );
        }

        this.players.clear();
        int i = 0;
        for (final Charakter c : charakterSetup.getRandedCharakters()) {
            final long randedUserId = rand.get(i);
            this.players.add(new Player(randedUserId, this.channelId, this.guildId, c.alignment, c.role, i + 1));
            i++;
        }
    }

    protected PrivateGuild allocatePrivateGuild() throws UserFriendlyException {
        PrivateGuild pg = Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.poll();
        if (pg == null) {
            Wolfia.handleOutputMessage(this.channelId,
                    "Acquiring a private server for the wolves...this may take a while.");
            log.error("Ran out of free private guilds. Please add moar.");
            try { //oh yeah...we are waiting till infinity if necessary
                pg = Wolfia.AVAILABLE_PRIVATE_GUILD_QUEUE.take();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for a private server.");
                throw new UserFriendlyException("Could not allocate a private server.");
            }
        }
        return pg;
    }

    /**
     * Prepares the channel for a moderated game
     *
     * @throws PermissionException if the bot is missing permissions to edit permission overrides for members and roles
     */
    protected void prepareChannel() throws PermissionException {
        final TextChannel channel = getThisChannel();
        final Guild g = channel.getGuild();

        // - ensure write access for the bot in the game channel
        // this can be done with complete() as most of the time (after the first game) it will already be in place
        // and will prevent messages getting lost due to queue() sometimes taking a while
        RoleAndPermissionUtils.grant(channel, g.getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).complete();

        // - no writing access and reaction adding for @everyone/access role in the game channel during the game
        RoleAndPermissionUtils.deny(channel, g.getRoleById(this.accessRoleId),
                Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue(null, Wolfia.defaultOnFail());
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

        final TextChannel channel = Wolfia.getTextChannelById(this.channelId);
        if (channel == null) {
            //we probably left the guild
            log.warn("Could not find channel {} to reset roles and permissions in there", this.channelId);
            return;
        }
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
            try {
                this.wolfChat.endUsage();
            } catch (final IllegalStateException ignored) {
                //dont really care about this one, its fine if usage has been stopped already
            }
        }
        this.executor.shutdownNow();
        resetRolesAndPermissions();
    }

    //public for eval usage
    public void destroy(final Throwable reason) {
        String reasonMessage = "No reason provided";
        if (reason != null) reasonMessage = reason.getMessage();
        final String logMessage = String.format("Game in channel %s destroyed due to %s", this.channelId, reasonMessage);
        if (reason instanceof UserFriendlyException) {
            log.info(logMessage, reason);
        } else {
            log.error(logMessage, reason);
        }
        cleanUp();
        Games.remove(this);
        final TextChannel channel = Wolfia.getTextChannelById(this.channelId);
        if (channel != null) {
            Wolfia.handleOutputMessage(channel,
                    "Game has been stopped due to:\n`%s`\nSorry about that. The issue has been logged and will hopefully be fixed soon." +
                            "\nFeel free to join the Wolfia Lounge meanwhile through `%s` for direct support with the issue.",
                    reasonMessage, Config.PREFIX + mainTrigger(HelpCommand.class));
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
    protected boolean isGameOver(final boolean... wwFlair) {
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
            this.gameStats.addAction(simpleAction(Wolfia.getSelfUser().getIdLong(), Actions.GAMEEND, -1));
            this.gameStats.setEndTime(System.currentTimeMillis());

            if (villageWins) {
                this.gameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.VILLAGE)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            } else {
                //woofs win
                this.gameStats.getStartingTeams().stream()
                        .filter(t -> t.getAlignment() == Alignments.WOLF)
                        .findFirst()
                        .ifPresent(t -> t.setWinner(true));
            }
            try {
                Wolfia.getInstance().dbWrapper.persist(this.gameStats);
                out += String.format("%nThis game's id is **%s**, you can watch its replay with `%s %s`",
                        this.gameStats.getId(), Config.PREFIX + mainTrigger(ReplayCommand.class), this.gameStats.getId());
            } catch (final DatabaseException e) {
                log.error("Db blew up saving game stats", e);
                out += "The database it not available currently, a replay of this game will not be available.";
            }
            cleanUp();
            final TextChannel channel = getThisChannel();
            DiscordLogger.getLogger().log("%s `%s` Game **#%s** ended in guild **%s** `%s`, channel **#%s** `%s`, **%s %s %s** players",
                    Emojis.END, TextchatUtils.berlinTime(), this.gameStats.getId(),
                    channel.getGuild().getName(), channel.getGuild().getIdLong(),
                    channel.getName(), channel.getIdLong(), Games.getInfo(this).textRep(), this.mode.textRep, this.players.size());
            // removing the game from the registry has to be the very last statement, since if a restart is queued, it
            // waits for an empty games registry
            Wolfia.handleOutputMessage(this.channelId,
                    ignoredMessage -> Games.remove(this),
                    throwable -> {
                        log.error("Failed to send last message of game #{}", this.gameStats.getId(), throwable);
                        Games.remove(this);
                    },
                    "%s", out);
            return true;
        }

        return false;
    }


    protected EmbedBuilder listLivingPlayersWithNumbers(final Player... except) {
        final NiceEmbedBuilder neb = new NiceEmbedBuilder();
        final TextChannel tc = getThisChannel();
        final Guild g = tc.getGuild();
        neb.setTitle("Living players");
        neb.setDescription("Game: " + Games.getInfo(this).textRep() + " " + this.mode.textRep + " on " + g.getName() + " in #" + tc.getName());

        final NiceEmbedBuilder.ChunkingField list = new NiceEmbedBuilder.ChunkingField("", true);
        final Set<Player> dontAdd = new HashSet<>(Arrays.asList(except));
        for (final Player p : getLivingPlayers()) {
            if (!dontAdd.contains(p)) {
                list.add(p.numberAsEmojis() + " " + p.getBothNamesFormatted(), true);
            }
        }
        neb.addField(list);
        return neb;
    }

    //an way to create ActionStats object with a bunch of default/automatically generated values, like time stamps
    protected abstract ActionStats simpleAction(final long actor, final Actions action, final long target);

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
     * @param channelId    main channel where the game shall run
     * @param mode         the chosen game mode
     * @param innedPlayers the players who signed up
     */
    public abstract void start(long channelId, GameInfo.GameMode mode, Set<Long> innedPlayers) throws DatabaseException;

    /**
     * Let the game handle a command a user issued
     *
     * @param command     the issued command
     * @param commandInfo the context of the issued command
     * @return true if the command was executed successful
     * @throws IllegalGameStateException if the command entered led to an illegal game state
     */
    public abstract boolean issueCommand(GameCommand command, CommandParser.CommandContainer commandInfo)
            throws IllegalGameStateException;
}
