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

package space.npstr.wolfia.game.mafia;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.MessageContext;
import space.npstr.wolfia.commands.game.RolePmCommand;
import space.npstr.wolfia.commands.ingame.CheckCommand;
import space.npstr.wolfia.commands.ingame.HohohoCommand;
import space.npstr.wolfia.commands.ingame.ItemsCommand;
import space.npstr.wolfia.commands.ingame.NightkillCommand;
import space.npstr.wolfia.commands.ingame.OpenPresentCommand;
import space.npstr.wolfia.commands.ingame.ShootCommand;
import space.npstr.wolfia.commands.ingame.UnvoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCommand;
import space.npstr.wolfia.commands.ingame.VoteCountCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.stats.ActionStats;
import space.npstr.wolfia.domain.stats.GameStats;
import space.npstr.wolfia.domain.stats.PlayerStats;
import space.npstr.wolfia.domain.stats.TeamStats;
import space.npstr.wolfia.events.UpdatingReactionListener;
import space.npstr.wolfia.game.Game;
import space.npstr.wolfia.game.GameInfo;
import space.npstr.wolfia.game.GameResources;
import space.npstr.wolfia.game.GameUtils;
import space.npstr.wolfia.game.Player;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Item;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.game.definitions.Roles;
import space.npstr.wolfia.game.exceptions.DayEndedAlreadyException;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.game.tools.NiceEmbedBuilder;
import space.npstr.wolfia.game.tools.VotingBuilder;
import space.npstr.wolfia.utils.PeriodicTimer;
import space.npstr.wolfia.utils.UserFriendlyException;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.RoleAndPermissionUtils;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import static java.util.Objects.requireNonNull;

/**
 * This is it, the actual werewolf/mafia game!
 */
public class Mafia extends Game {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Mafia.class);

    private long dayLengthMillis = TimeUnit.MINUTES.toMillis(10); //10 minutes default
    private final long nightLengthMillis = TimeUnit.MINUTES.toMillis(1); //1 minute default
    //current cycle and phase, describing n0, d1, n1, d2, n2 etc...
    private int cycle = 0;
    private Phase phase = Phase.NIGHT;
    private long phaseStarted = -1;

    private final Map<Player, Player> votes = new LinkedHashMap<>();//using linked to keep first votes at the top
    private final Map<Player, ActionStats> voteActions = new HashMap<>();

    private final Map<Player, Player> nightkillVotes = new LinkedHashMap<>();//using linked to keep first votes at the top
    private final Map<Player, ActionStats> nightKillVoteActions = new HashMap<>();
    private final Map<Player, ActionStats> nightActions = new HashMap<>();

    private Future<?> phaseEndTimer;
    private Future<?> phaseEndReminder;
    private final VotingBuilder votingBuilder = new VotingBuilder()
            .unvoteEmoji(Emojis.X)
            .header("Day ends in **%timeleft** with a lynch.")
            .notes(String.format("**Use `%s` to cast a vote on a player.**"
                    + "%nOnly your last vote will be counted."
                    + "%nMajority is enabled.", WolfiaConfig.DEFAULT_PREFIX + VoteCommand.TRIGGER));

    private final VotingBuilder nightKillVotingBuilder = new VotingBuilder()
            .unvoteEmoji(Emojis.X)
            .header("Night ends in **%timeleft**.")
            .notes(String.format("**Use `%s` to cast a vote on a player.**"
                    + "%nOnly your last vote will be counted.", WolfiaConfig.DEFAULT_PREFIX + NightkillCommand.TRIGGER));

    protected Mafia(GameResources gameResources) {
        super(gameResources);
    }

    @Override
    public void setDayLength(Duration dayLength) {
        if (this.running) {
            throw new IllegalStateException("Cannot change day length externally while the game is running");
        }
        this.dayLengthMillis = dayLength.toMillis();
    }

    @Override
    public EmbedBuilder getStatus() {
        NiceEmbedBuilder neb = NiceEmbedBuilder.defaultBuilder();
        neb.addField("Game", Games.MAFIA.textRep + " " + this.mode.textRep, true);
        if (!this.running) {
            neb.addField("", "**Game is not running**", false);
            return neb;
        }
        neb.addField("Phase", this.phase.textRep + " " + this.cycle, true);
        long timeLeft = this.phaseStarted + (this.phase == Phase.DAY ? this.dayLengthMillis : this.nightLengthMillis) - System.currentTimeMillis();
        neb.addField("Time left", TextchatUtils.formatMillis(timeLeft), true);

        NiceEmbedBuilder.ChunkingField living = new NiceEmbedBuilder.ChunkingField("Living Players", true);
        getLivingPlayers().forEach(p -> living.add(p.numberAsEmojis() + " " + p.bothNamesFormatted(), true));
        neb.addField(living);

        StringBuilder sb = new StringBuilder();
        getLivingWolves().forEach(w -> sb.append(Emojis.SPY));
        neb.addField("Living Mafia", sb.toString(), true);

        return neb;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void start(long channelId, GameInfo.GameMode mode, Set<Long> innedPlayers) {
        try {//wrap into our own exceptions
            doArgumentChecksAndSet(channelId, mode, innedPlayers);
        } catch (IllegalArgumentException e) {
            throw new UserFriendlyException(e.getMessage(), e);
        }

        doPermissionCheckAndPrepareChannel(true); //all werewolf games are moderated

        this.cycle = 0;
        this.phase = Phase.NIGHT;

        // - rand the characters
        randCharacters(innedPlayers);

        //get a hold of a private server...
        this.wolfChat = allocatePrivateRoom();
        this.wolfChat.beginUsage(getWolvesIds());

        TextChannel gameChannel = fetchGameChannel();
        //inform each player about his role
        String inviteLink = TextchatUtils.getOrCreateInviteLinkForChannel(gameChannel);
        String wolfchatInvite = this.wolfChat.getInvite();
        StringBuilder mafiaTeamNames = new StringBuilder("Your team is:\n");
        String guildChannelAndInvite = String.format("Guild/Server: **%s**%nMain channel: **#%s** %s%n", //invite that may be empty
                gameChannel.getGuild().getName(), gameChannel.getName(), inviteLink);

        for (Player player : getWolves()) {
            mafiaTeamNames.append(player.bothNamesFormatted()).append("\n");
        }

        for (Player player : this.players) {
            StringBuilder rolePm = new StringBuilder()
                    .append("Hi ").append(player.getName()).append("!\n")
                    .append(player.alignment.rolePmBlockMaf).append("\n")
                    .append(player.role.rolePmBlock).append("\n");
            if (player.isBaddie()) {
                rolePm.append(mafiaTeamNames);
                rolePm.append("Wolfchat: ").append(wolfchatInvite).append("\n");
                addToBaddieGuild(player);
            }
            rolePm.append(guildChannelAndInvite);

            player.setRolePm(rolePm.toString());
            player.sendMessage(rolePm.toString(),
                    e -> RestActions.sendMessage(gameChannel,
                            String.format("%s, **I cannot send you a private message**, please adjust your privacy settings " +
                                            "and/or unblock me, then issue `%s` to receive your role PM.",
                                    player.asMention(), WolfiaConfig.DEFAULT_PREFIX + RolePmCommand.TRIGGER))
            );
        }


        Guild g = gameChannel.getGuild();
        //set up stats objects
        this.gameStats = new GameStats(g.getIdLong(), g.getName(), this.channelId, gameChannel.getName(),
                Games.MAFIA, this.mode, this.players.size());
        Map<Alignments, TeamStats> teams = new EnumMap<>(Alignments.class);
        for (Player player : this.players) {
            Alignments alignment = player.alignment;
            TeamStats team = teams.getOrDefault(alignment,
                    new TeamStats(this.gameStats, alignment, alignment.textRepMaf, -1));
            PlayerStats ps = new PlayerStats(team, player.userId,
                    player.getNick(), alignment, player.role);
            this.playersStats.put(player.userId, ps);
            team.addPlayer(ps);
            teams.put(alignment, team);
        }
        for (TeamStats team : teams.values()) {
            team.setTeamSize(team.getPlayers().size());
            this.gameStats.addTeam(team);
        }

        // - start the game
        String info = Games.getInfo(this).textRep();
        log.info("Game started in guild {} {}, channel #{} {}, {} {} {} players",
                g.getName(), g.getIdLong(), gameChannel.getName(), gameChannel.getIdLong(),
                info, mode.textRep, this.players.size());
        this.running = true;
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.GAMESTART, -1));
        //mention the players in the thread
        RestActions.sendMessage(gameChannel, "Game has started!\n" + listLivingPlayers());

        //start the time only after the message was actually sent
        Consumer whenDone = aVoid -> scheduleIfGameStillRuns(this::startDay, Duration.ofSeconds(20));
        RestActions.sendMessage(gameChannel, "Time to read your role PMs! Day starts in 20 seconds.", whenDone, whenDone);
    }

    @Override
    public boolean issueCommand(CommandContext context) {
        Player invoker;
        try {
            invoker = getPlayer(context.invoker.getIdLong());
        } catch (IllegalGameStateException e) {
            context.replyWithMention("shush, you're not playing in this game!");
            return false;
        }
        if (invoker.isDead()) {
            context.replyWithMention("shush, you're dead!");
            return false;
        }

        //we can compare classes with == as long as we are using the same classloader (which we are)
        Optional<Guild> guild = context.getGuild();
        if (context.command instanceof VoteCommand) {
            if (context.channel.getIdLong() != this.channelId) {
                context.replyWithMention("you can issue that command only in the main game channel.");
                return false; //ignore vote commands not in game chat
            }
            Player candidate = GameUtils.identifyPlayer(this.players, context);
            if (candidate == null) return false;

            return vote(invoker, candidate, context);
        } else if (context.command instanceof UnvoteCommand) {
            if (context.channel.getIdLong() != this.channelId) {
                context.replyWithMention("you can issue that command only in the main game channel.");
                return false; //ignore vote commands not in game chat
            }

            if (guild.isPresent() && guild.get().getIdLong() == this.wolfChat.getGuildId()) {
                return nkUnvote(invoker, context);
            }

            return unvote(invoker, context);
        } else if (context.command instanceof CheckCommand) {

            if (context.channel.getType() != ChannelType.PRIVATE) {
                context.replyWithMention("checks can only be issued in private messages.");
                return false;
            }

            if (this.phase != Phase.NIGHT) {
                context.replyWithMention("checks can only be issued during the night.");
                return false;
            }

            if (invoker.role != Roles.COP && !invoker.hasItemOfType(Item.ItemType.MAGNIFIER)) {
                context.replyWithMention("you can't issue a check when you aren't a cop and don't own a " + Item.ItemType.MAGNIFIER + ".");
                return false;
            }

            Player target = GameUtils.identifyPlayer(this.players, context);
            if (target == null) return false;

            return check(invoker, target, context);
        } else if (context.command instanceof HohohoCommand) {
            if (context.channel.getType() != ChannelType.PRIVATE) {
                context.replyWithMention("presents can only be given in private messages.");
                return false;
            }

            if (this.phase != Phase.NIGHT) {
                context.replyWithMention("presents can only be given during the night.");
                return false;
            }

            if (invoker.role != Roles.SANTA) {
                context.replyWithMention("you can't send presents when you aren't santa.");
                return false;
            }

            Player target = GameUtils.identifyPlayer(this.players, context);
            if (target == null) return false;

            return givePresent(invoker, target, context);
        } else if (context.command instanceof OpenPresentCommand) {
            if (context.channel.getType() != ChannelType.PRIVATE) {
                context.replyWithMention("presents can only be opened in private messages.");
                return false;
            }

            return openPresent(invoker, context);
        } else if (context.command instanceof ShootCommand) {
            if (context.channel.getType() != ChannelType.PRIVATE) {
                context.replyWithMention("shots in this game/mode can only be issued in private messages.");
                return false;
            }

            if (!invoker.hasItemOfType(Item.ItemType.GUN)) {
                context.reply("You can't shoot if you don't own a " + Item.ItemType.GUN + ".");
                return false;
            }

            if (this.phase != Phase.DAY) {
                context.reply("You can only shoot during the day.");
                return false;
            }

            Player target = GameUtils.identifyPlayer(this.players, context);
            if (target == null) return false;

            return shoot(invoker, target, context);
        } else if (context.command instanceof VoteCountCommand) {

            //wolves asked for one, give them a votecount of their nk votes
            if (this.phase == Phase.NIGHT && guild.isPresent() && guild.get().getIdLong() == this.wolfChat.getGuildId()) {
                context.reply(this.nightKillVotingBuilder.getEmbed(new HashMap<>(this.nightkillVotes)).build());
                return true;
            }

            if (this.phase != Phase.DAY) {
                context.replyWithMention("vote counts are only shown during the day phase.");
                return false;
            }
            context.reply(this.votingBuilder.getEmbed(new HashMap<>(this.votes)).build());
            return true;

        } else if (context.command instanceof NightkillCommand) {
            //equivalent to the vote command m just for baddies in the night

            Player candidate = GameUtils.identifyPlayer(this.players, context);
            if (candidate == null) return false;

            return nkVote(invoker, candidate, context);
        } else {
            context.replyWithMention("the '" + context.command.getTrigger() + "' command is not part of this game.");
            return false;
        }
    }

    private boolean vote(Player voter, Player candidate, MessageContext context) {

        TextChannel gameChannel = fetchGameChannel();
        if (this.phase != Phase.DAY) {
            context.reply(voter.asMention() + ", you can only vote during the day.");
            return false;
        }

        if (candidate.isDead()) {
            context.reply(voter.asMention() + ", you can't vote for a dead player.");
            return false;
        }

        RestActions.sendMessage(gameChannel, String.format("%s votes %s for lynch.", voter.asMention(), candidate.asMention()));

        synchronized (this.votes) {
            this.votes.remove(voter);
            this.votes.put(voter, candidate);
            this.voteActions.put(voter, simpleAction(voter.userId, Actions.VOTELYNCH, candidate.userId));

            //check for majj
            int livingPlayersCount = getLivingPlayers().size();
            int majThreshold = (livingPlayersCount / 2);
            long mostVotes = GameUtils.mostVotes(this.votes);
            if (mostVotes > majThreshold) {
                RestActions.sendMessage(gameChannel, Emojis.ANGRY_BUBBLE + "Majority was reached!");
                try {
                    endDay();
                } catch (DayEndedAlreadyException ignored) {
                    // ignored
                }
            }
        }
        return true;
    }

    private boolean unvote(Player unvoter, MessageContext context, boolean... silent) {

        boolean shutUp = silent.length > 0 && silent[0];
        TextChannel gameChannel = fetchGameChannel();
        if (this.phase != Phase.DAY) {
            if (!shutUp)
                context.reply(unvoter.asMention() + ", you can only unvote during the day.");
            return false;
        }

        Player unvoted;
        synchronized (this.votes) {
            if (this.votes.get(unvoter) == null) {
                if (!shutUp)
                    context.reply(unvoter.asMention() + ", you can't unvote if you aren't voting in the first place.");
                return false;
            }
            unvoted = this.votes.remove(unvoter);
            this.voteActions.remove(unvoter);
        }

        if (!shutUp) {
            RestActions.sendMessage(gameChannel, String.format("%s unvoted %s.",
                    unvoter.asMention(), unvoted.asMention()));
        }
        return true;
    }

    private boolean check(Player invoker, Player target, CommandContext context) {
        if (target.isDead()) {
            context.reply("You can't check a dead player.");
            return false;
        }

        if (target.equals(invoker)) {
            context.reply("_Check yourself before you wreck yourself._\nYou can't check yourself, please check another player of the game.");
            return false;
        }

        this.nightActions.put(invoker, simpleAction(invoker.userId, Actions.CHECK, target.userId));
        context.reply("You are checking " + target.bothNamesFormatted() + " tonight");
        return true;
    }

    private boolean givePresent(Player invoker, Player target, CommandContext context) {
        if (target.isDead()) {
            context.reply("You can't give a present to a dead player.");
            return false;
        }

        if (target.equals(invoker)) {
            context.reply("Sorry, you've been very naughty. You can't give a present to yourself.");
            return false;
        }

        this.nightActions.put(invoker, simpleAction(invoker.userId, Actions.GIVE_PRESENT, target.userId));
        context.reply("You are climbing down " + target.bothNamesFormatted() + "'s chimney tonight and leaving them a " + Item.ItemType.PRESENT);
        return true;
    }

    private boolean openPresent(Player invoker, CommandContext context) {

        Item hasPresent = null;
        for (Item item : invoker.items) {
            if (item.itemType == Item.ItemType.PRESENT) {
                hasPresent = item;
                break;
            }
        }
        if (hasPresent == null) {
            String message = String.format("You don't have any presents to open. Say `%s` to see your items.",
                    WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER);
            context.reply(message);
            return false;
        } else {
            invoker.items.remove(hasPresent);
        }

        Item.ItemType openedPresent = GameUtils.rand(Arrays.asList(Item.ItemType.GUN, Item.ItemType.MAGNIFIER, Item.ItemType.BOMB, Item.ItemType.ANGEL));
        invoker.items.add(new Item(hasPresent.sourceId, openedPresent));
        this.gameStats.addAction(simpleAction(invoker.userId, Actions.OPEN_PRESENT, invoker.userId).setAdditionalInfo(openedPresent.name()));

        context.reply("You received a " + openedPresent.emoji + "! This has the following effect:\n" + openedPresent.explanation);

        if (openedPresent == Item.ItemType.BOMB) {

            //use up an angel if this person has one
            Optional<Item> angel = invoker.items.stream().filter(i -> i.itemType == Item.ItemType.ANGEL).findAny();
            if (angel.isPresent()) {
                invoker.items.remove(angel.get());
                invoker.sendMessage(String.format("Your present contained a %s, but luckily one of your %ss saved you! Say `%s` to see what items you have left.",
                        Item.ItemType.BOMB, Item.ItemType.ANGEL, WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER), RestActions.defaultOnFail());
                RestActions.sendMessage(fetchGameChannel(), "An explosion is heard, but nobody dies.");
                return true;
            }

            //noinspection UnnecessaryLocalVariable
            Player dying = invoker;

            try {
                dying.kill();
            } catch (IllegalGameStateException e) {
                //lets ignore this for now and just log it
                log.error("Dead player got a bomb from present", e);
            }
            this.gameStats.addAction(simpleAction(hasPresent.sourceId, Actions.DEATH, dying.userId));

            //remove votes of dead player and ppl voting the dead player
            clearVotesForPlayer(dying, context);
            clearNkVotesForPlayer(dying, context);

            //remove writing permissions
            TextChannel gameChannel = fetchGameChannel();
            RoleAndPermissionUtils.deny(gameChannel, gameChannel.getGuild().getMemberById(dying.userId),
                    Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());

            //send info
            String message = String.format("%s %s opened a %s and found a lit %s inside, killing them immediately.%n%s",
                    Emojis.BOOM, dying.asMention(), Item.ItemType.PRESENT, Item.ItemType.BOMB, getReveal(dying));
            RestActions.sendMessage(gameChannel, message);
            if (this.phase == Phase.NIGHT) {
                RestActions.sendMessage(fetchBaddieChannel(), message);
            }
            isGameOver();
        }
        return true;
    }

    private boolean shoot(Player invoker, Player target, CommandContext context) {
        if (invoker.equals(target)) {
            context.reply(String.format("Please don't %s yourself, that would make a big mess.", Emojis.GUN));
            return false;
        } else if (target.isDead()) {
            context.reply(String.format("You have to %s a living player of this game.", Emojis.GUN));
            context.reply(listLivingPlayersWithNumbers(invoker));
            return false;
        }

        Player dying = target;

        this.gameStats.addAction(simpleAction(invoker.userId, Actions.SHOOT, dying.userId));

        //use up a gun if the invoker has one
        Optional<Item> gun = invoker.items.stream().filter(i -> i.itemType == Item.ItemType.GUN).findAny();
        if (gun.isPresent()) {
            invoker.items.remove(gun.get());
            invoker.sendMessage(String.format("You used up one of your %ss. Say `%s` to see what items you have left.",
                    Item.ItemType.GUN, WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER), RestActions.defaultOnFail());
        }

        //use up an angel if the target has one
        Optional<Item> angel = target.items.stream().filter(i -> i.itemType == Item.ItemType.ANGEL).findAny();
        if (angel.isPresent()) {
            target.items.remove(angel.get());
            target.sendMessage(String.format("One of your %ss saved you! Say `%s` to see what items you have left.",
                    Item.ItemType.ANGEL, WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER), RestActions.defaultOnFail());
            RestActions.sendMessage(fetchGameChannel(), "A shot rings out, but nobody dies.");
            return true;
        }

        try {
            dying.kill();
        } catch (IllegalGameStateException e) {
            //lets ignore this for now and just log it
            log.error("Dead player got a bomb from present", e);
        }
        this.gameStats.addAction(simpleAction(invoker.userId, Actions.DEATH, dying.userId));


        //remove votes of dead player and ppl voting the dead player
        clearVotesForPlayer(dying, context);
        clearNkVotesForPlayer(dying, context);

        //remove writing permissions
        TextChannel gameChannel = fetchGameChannel();
        RoleAndPermissionUtils.deny(gameChannel, gameChannel.getGuild().getMemberById(dying.userId),
                Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());

        //send info
        String message = String.format("%s has been shot! They die immediately.%n%s",
                dying.asMention(), getReveal(dying));
        RestActions.sendMessage(gameChannel, message);
        if (this.phase == Phase.NIGHT) {
            RestActions.sendMessage(fetchBaddieChannel(), message);
        }
        isGameOver();
        return true;
    }

    private void clearVotesForPlayer(Player player, MessageContext context) {
        Set<Player> toUnvote = new HashSet<>();
        for (Map.Entry<Player, Player> vote : this.votes.entrySet()) {
            Player voter = vote.getKey();
            Player candidate = vote.getValue();
            if (voter.equals(player) || candidate.equals(player)) {
                toUnvote.add(voter);
            }
        }
        for (Player unvoter : toUnvote) {
            unvote(unvoter, context, true);
        }
    }

    private void clearNkVotesForPlayer(Player player, MessageContext context) {
        Set<Player> toUnvoteNk = new HashSet<>();
        for (Map.Entry<Player, Player> vote : this.nightkillVotes.entrySet()) {
            Player voter = vote.getKey();
            Player candidate = vote.getValue();
            if (voter.equals(player) || candidate.equals(player)) {
                toUnvoteNk.add(voter);
            }
        }
        for (Player unvoter : toUnvoteNk) {
            nkUnvote(unvoter, context, true);
        }
    }

    //simplifies the giant constructor of an action by providing it with game/mode specific defaults
    @Override
    protected ActionStats simpleAction(long actor, Actions action, long target) {
        long now = System.currentTimeMillis();
        return new ActionStats(this.gameStats, this.actionOrder.incrementAndGet(),
                now, now, this.cycle, this.phase, actor, action, target, null);
    }

    private void startDay() {
        this.cycle++;
        this.phase = Phase.DAY;
        this.phaseStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.DAYSTART, -1));

        this.votes.clear();
        this.voteActions.clear();
        List<Player> living = getLivingPlayers();
        this.votingBuilder.endTime(this.phaseStarted + this.dayLengthMillis)
                .possibleVoters(living)
                .possibleCandidates(living);

        //open channel
        TextChannel gameChannel = fetchGameChannel();
        RestActions.sendMessage(gameChannel, String.format("Day %s started! You have %s minutes to discuss. You may vote a"
                        + " player for lynch with `%s`. You can see the current votecount with `%s`."
                        + "\nIf a player is voted by more than half the living players (majority), they will be lynched immediately!",
                this.cycle, this.dayLengthMillis / 60000, WolfiaConfig.DEFAULT_PREFIX + VoteCommand.TRIGGER,
                WolfiaConfig.DEFAULT_PREFIX + VoteCountCommand.TRIGGER));
        for (Player player : living) {
            RoleAndPermissionUtils.grant(gameChannel, gameChannel.getGuild().getMemberById(player.userId),
                    Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());
        }

        //set a timer that calls endDay()
        this.phaseEndTimer = scheduleIfGameStillRuns(() -> {
            try {
                this.endDay();
            } catch (DayEndedAlreadyException ignored) {
                // ignored
            }
        }, Duration.ofMillis(this.dayLengthMillis));
        this.phaseEndReminder = scheduleIfGameStillRuns(() -> RestActions.sendMessage(gameChannel, "One minute left until day end!"),
                Duration.ofMillis(this.dayLengthMillis - 60000));
    }

    private void endDay() throws DayEndedAlreadyException {
        synchronized (this.hasDayEnded) {
            //check if this is a valid call
            if (this.hasDayEnded.contains(this.cycle)) {
                throw new DayEndedAlreadyException();
            }
            this.hasDayEnded.add(this.cycle);
        }
        if (this.phaseEndTimer != null) this.phaseEndTimer.cancel(false);
        if (this.phaseEndReminder != null) this.phaseEndReminder.cancel(false);

        TextChannel gameChannel = fetchGameChannel();

        List<Player> livingPlayers = getLivingPlayers();
        //close channel
        for (Player livingPlayer : livingPlayers) {
            RoleAndPermissionUtils.deny(gameChannel, gameChannel.getGuild().getMemberById(livingPlayer.userId),
                    Permission.MESSAGE_WRITE).queue(null, RestActions.defaultOnFail());
        }

        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.DAYEND, -1));
        synchronized (this.votes) {
            RestActions.sendMessage(gameChannel, this.votingBuilder.getFinalEmbed(this.votes, this.phase, this.cycle).build());
            List<Player> lynchCandidates = GameUtils.mostVoted(this.votes, livingPlayers);
            boolean randedLynch = false;
            Player lynchCandidate;
            if (lynchCandidates.size() > 1) {
                randedLynch = true;
                lynchCandidate = GameUtils.rand(lynchCandidates);
            } else {
                lynchCandidate = lynchCandidates.get(0);
            }

            try {
                lynchCandidate.kill();
                this.gameStats.addAction(simpleAction(-3, Actions.LYNCH, lynchCandidate.userId));
            } catch (IllegalGameStateException | NullPointerException e) {
                //should not happen, but if it does, kill the game
                this.destroy(e);
                return;
            }

            long votesAmount = this.votes.values().stream().filter(p -> p.userId == lynchCandidate.userId).count();
            RestActions.sendMessage(gameChannel, String.format("%s has been lynched%s with %s votes on them!%nThey were **%s %s** %s",
                    lynchCandidate.asMention(), randedLynch ? " at random due to a tie" : "", votesAmount,
                    lynchCandidate.alignment.textRepMaf, lynchCandidate.role.textRep, lynchCandidate.getCharakterEmoji()));
            this.gameStats.addActions(this.voteActions.values());
        }

        if (!isGameOver()) {
            startNight();
        }
    }

    private void postUpdatingNightMessage() {
        String basic = "Night falls...\n";
        RestActions.sendMessage(fetchGameChannel(), basic + nightTimeLeft(),
                m -> new PeriodicTimer(
                        resources.getExecutor(),
                        TimeUnit.SECONDS.toMillis(5),
                        onUpdate -> RestActions.editMessage(m, basic + nightTimeLeft()),
                        this.phaseStarted + this.nightLengthMillis - System.currentTimeMillis(),
                        onDestruction -> RestActions.editMessage(m, basic + "Dawn breaks!")
                ));
    }

    private String nightTimeLeft() {
        return "Time left: " + TextchatUtils.formatMillis(this.phaseStarted + this.nightLengthMillis - System.currentTimeMillis());
    }

    private void startNight() {
        this.phase = Phase.NIGHT;
        this.phaseStarted = System.currentTimeMillis();
        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.NIGHTSTART, -1));

        this.nightActions.clear();

        postUpdatingNightMessage();

        //post a voting embed for the wolfs in wolfchat
        TextChannel wolfchatChannel = fetchBaddieChannel();

        this.nightkillVotes.clear();
        this.nightKillVoteActions.clear();

        this.nightKillVotingBuilder.endTime(this.phaseStarted + this.nightLengthMillis)
                .possibleVoters(getLivingWolves())
                .possibleCandidates(getLivingVillage());


        RestActions.sendMessage(wolfchatChannel, "Nightkill voting!\n" + String.join(", ", getLivingWolvesMentions()),
                m -> RestActions.sendMessage(wolfchatChannel, this.nightKillVotingBuilder.getEmbed(this.nightkillVotes).build(), message -> {
                    ShardManager shardManager = requireNonNull(message.getJDA().getShardManager());
                    shardManager.addEventListener(new UpdatingReactionListener(
                            shardManager,
                            resources.getExecutor(),
                            message,
                            this::isLivingWolf,
                            __ -> {
                            },//todo move away from using a reaction listener
                            this.nightLengthMillis,
                            //on destruction
                            aVoid -> {
                                if (!this.running) {//game ended meanwhile.
                                    return;
                                }
                                message.clearReactions().queue(null, RestActions.defaultOnFail());
                                synchronized (this.nightkillVotes) {
                                    RestActions.editMessage(message, this.nightKillVotingBuilder.getFinalEmbed(this.nightkillVotes, this.phase, this.cycle).build());
                                    Player nightKillCandidate = GameUtils.rand(GameUtils.mostVoted(this.nightkillVotes, getLivingVillage()));

                                    TextChannel textChannel = shardManager.getTextChannelById(this.channelId);
                                    String invite = textChannel == null ? ""
                                            : TextchatUtils.getOrCreateInviteLinkForChannel(textChannel);
                                    RestActions.sendMessage(wolfchatChannel, String.format(
                                            "%n@here, %s will be killed! Game about to start/continue, get back to the main chat.%n%s",
                                            nightKillCandidate.bothNamesFormatted(), invite));
                                    this.gameStats.addActions(this.nightKillVoteActions.values());

                                    endNight(nightKillCandidate);
                                }
                            },
                            //update every few seconds
                            TimeUnit.SECONDS.toMillis(10),
                            aVoid -> RestActions.editMessage(message, this.nightKillVotingBuilder.getEmbed(this.nightkillVotes).build())
                    ));
                })
        );


        //notify other roles of their possible night actions

        for (Player p : getLivingPlayers()) {
            //cop
            if (p.role == Roles.COP) {
                EmbedBuilder livingPlayersWithNumbers = listLivingPlayersWithNumbers(p);
                String out = String.format("**You are a cop. Use `%s [name or number]` to check the alignment of a player.**%n" +
                                "You will receive the result at the end of the night for the last submitted target. " +
                                "If you do not submit a check, it will be randed.",
                        WolfiaConfig.DEFAULT_PREFIX + CheckCommand.TRIGGER);
                livingPlayersWithNumbers.addField("", out, false);
                Collection<Long> randCopTargets = getLivingPlayerIds();
                randCopTargets.remove(p.userId);//dont randomly check themselves
                this.nightActions.put(p, simpleAction(p.userId, Actions.CHECK, GameUtils.rand(randCopTargets)));//preset a random action
                p.sendMessage(livingPlayersWithNumbers.build(), RestActions.defaultOnFail());
            } else if (p.role == Roles.SANTA) {
                EmbedBuilder livingPlayersWithNumbers = listLivingPlayersWithNumbers(p);
                String out = String.format("**You are Santa Claus. Use `%s [name or number]` to send a %s to another player.**%n"
                                + "If they decide to open the present, it may contain one of the following things at random:"
                                + "\n" + Item.ItemType.GUN + " Allows the target player to shoot another player during the day."
                                + "\n" + Item.ItemType.MAGNIFIER + " Allows the target player to check another player's alignment during the night."
                                + "\n" + Item.ItemType.BOMB + " Kills the target player immediately."
                                + "\n" + Item.ItemType.ANGEL + " Protects the target from death once, but not from the lynch."
                                + "\n\nIf you don't submit an action, a random living player will receive the present.",
                        WolfiaConfig.DEFAULT_PREFIX + HohohoCommand.TRIGGER, Item.ItemType.PRESENT);
                livingPlayersWithNumbers.addField("", out, false);
                Collection<Long> randSantaTargets = getLivingPlayerIds();
                randSantaTargets.remove(p.userId);//dont randomly gift themselves
                this.nightActions.put(p, simpleAction(p.userId, Actions.GIVE_PRESENT, GameUtils.rand(randSantaTargets)));//preset a random action
                p.sendMessage(livingPlayersWithNumbers.build(), RestActions.defaultOnFail());
            }
        }
    }

    private boolean nkVote(Player voter, Player nightkillVote, CommandContext context) {

        if (this.phase != Phase.NIGHT) {
            context.replyWithMention("you can only vote during the night.");
            return false;
        }

        if (nightkillVote.isDead()) {
            context.replyWithMention("you can't vote a dead player for nightkill.");
            return false;
        }

        if (nightkillVote.isBaddie()) { //this needs to be revisited if multiple baddie faction become a thing
            context.replyWithMention("you can't vote to nightkill a fellow mafioso.");
            return false;
        }

        context.reply(String.format("%s votes %s for nightkill.", voter.asMention(), nightkillVote.asMention()));

        synchronized (this.nightkillVotes) {
            this.nightkillVotes.remove(voter);
            this.nightkillVotes.put(voter, nightkillVote);
            this.nightKillVoteActions.put(voter, simpleAction(voter.userId, Actions.VOTENIGHTKILL, nightkillVote.userId));
        }
        return true;
    }

    private boolean nkUnvote(Player unvoter, MessageContext context, boolean... silent) {
        boolean shutUp = silent.length > 0 && silent[0];

        if (this.phase != Phase.NIGHT) {
            if (!shutUp) {
                context.replyWithMention("you can only unvote during the night.");
            }
            return false;
        }

        Player unvoted;
        synchronized (this.nightkillVotes) {
            if (this.nightkillVotes.get(unvoter) == null) {
                if (!shutUp) {
                    context.replyWithMention("you can't unvote if you aren't voting in the first place.");
                }
                return false;
            }
            unvoted = this.nightkillVotes.remove(unvoter);
            this.nightKillVoteActions.remove(unvoter);
        }

        if (!shutUp) {
            RestActions.sendMessage(fetchBaddieChannel(), String.format("%s unvoted %s.", unvoter.asMention(), unvoted.asMention()));
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void endNight(Player nightKillCandidate) {

        this.gameStats.addAction(simpleAction(this.selfUserId, Actions.NIGHTEND, -1));

        for (ActionStats nightAction : this.nightActions.values()) {
            if (nightAction.getActionType() == Actions.CHECK) {
                try {
                    Player checker = getPlayer(nightAction.getActor());
                    Player checked = getPlayer(nightAction.getTarget());
                    checker.sendMessage(String.format("%s, you checked %s on night %s. Their alignment is **%s**",
                            checker.asMention(), checked.bothNamesFormatted(), this.cycle,
                            checked.alignment.textRepMaf), RestActions.defaultOnFail());
                    nightAction.setTimeStampHappened(System.currentTimeMillis());
                    this.gameStats.addAction(nightAction);

                    //use up a mag if this player has one
                    Optional<Item> mag = checker.items.stream().filter(i -> i.itemType == Item.ItemType.MAGNIFIER).findAny();
                    if (mag.isPresent()) {
                        checker.items.remove(mag.get());
                        checker.sendMessage(String.format("You used up your %s. Say `%s` to see what items you have left.",
                                Item.ItemType.MAGNIFIER, WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER), RestActions.defaultOnFail());
                    }
                } catch (IllegalGameStateException e) {
                    log.error("Checked player {} not a player of the ongoing game in {}.", nightAction.getTarget(), this.channelId);
                }
            } else if (nightAction.getActionType() == Actions.GIVE_PRESENT) {
                try {
                    Player receiver = getPlayer(nightAction.getTarget());
                    String message = String.format("Someone left a %s under your Xmas tree! Say `%s` to open it, if you dare.",
                            Item.ItemType.PRESENT.emoji, WolfiaConfig.DEFAULT_PREFIX + OpenPresentCommand.TRIGGER);
                    receiver.sendMessage(message, RestActions.defaultOnFail());
                    receiver.items.add(new Item(nightAction.getActor(), Item.ItemType.PRESENT));
                    this.gameStats.addAction(nightAction);
                } catch (IllegalGameStateException e) {
                    log.error("Player {} getting a present not a player of the ongoing game in {}.", nightAction.getTarget(), this.channelId);
                }
            } else {
                log.error("Unsupported night action encountered: {}", nightAction.getActionType());
            }
        }

        TextChannel gameChannel = fetchGameChannel();
        //use up an angel if the target has one
        Optional<Item> angel = nightKillCandidate.items.stream().filter(i -> i.itemType == Item.ItemType.ANGEL).findAny();
        if (angel.isPresent()) {
            nightKillCandidate.items.remove(angel.get());
            nightKillCandidate.sendMessage(String.format("One of your %ss saved you! Say `%s` to see what items you have left.",
                    Item.ItemType.ANGEL, WolfiaConfig.DEFAULT_PREFIX + ItemsCommand.TRIGGER), RestActions.defaultOnFail());
            RestActions.sendMessage(gameChannel, "Nobody died during the night.");
        } else {
            try {
                nightKillCandidate.kill();
                this.gameStats.addAction(simpleAction(-2, Actions.DEATH, nightKillCandidate.userId));
            } catch (IllegalGameStateException e) {
                //should not happen, but if it does, kill the game
                this.destroy(e);
                return;
            }
            RestActions.sendMessage(gameChannel, String.format("%s has died during the night!%n%s",
                    nightKillCandidate.asMention(), getReveal(nightKillCandidate)));
        }

        if (!isGameOver()) {
            //start the timer only after the message has actually been sent
            Consumer whenDone = aVoid -> scheduleIfGameStillRuns(this::startDay, Duration.ofSeconds(10));
            RestActions.sendMessage(gameChannel, String.format("Day starts in 10 seconds.%n%s",
                            String.join(", ", getLivingPlayerMentions())),
                    whenDone, whenDone);
        }
    }

    private static String getReveal(Player dying) {
        return String.format("They were **%s %s** %s", dying.alignment.textRepMaf, dying.role.textRep, dying.getCharakterEmoji());
    }
}
