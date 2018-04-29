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

package space.npstr.wolfia.db.entities.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.sqlsauce.entities.SaucedEntity;
import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.wolfia.game.definitions.Actions;
import space.npstr.wolfia.game.definitions.Alignments;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.definitions.Item;
import space.npstr.wolfia.game.definitions.Phase;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Created by napster on 30.05.17.
 * <p>
 * Describe an action that happened during a game
 */
@Entity
@Table(name = "stats_action")
public class ActionStats extends SaucedEntity<Long, ActionStats> {

    private static final Logger log = LoggerFactory.getLogger(ActionStats.class);
    private static final long serialVersionUID = -6803073458836067860L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_id", nullable = false)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameStats game;

    //chronological order of the actions
    //just a failsafe in case the List of actions in Game gets into an unsorted state
    @Column(name = "sequence", nullable = false) //order is a reserved keyword in postgres, so we use sequence instead
    private int order;

    // the difference between these two timestamps is the following: an action may be submitted before it actually
    // happens (example: nk gets submitted during the night, but actually "happens" when the day starts and results are
    // announced). these two timestamps try to capture that data as accurately as possible
    @Column(name = "submitted", nullable = false)
    private long timeStampSubmitted;

    @Column(name = "happened", nullable = false)
    private long timeStampHappened;

    //n0, d1 + n1, d2 + n2 etc
    @Column(name = "cycle", nullable = false)
    private int cycle;

    //day or night or whatever else, defined in the Phase enum
    @Column(name = "phase", nullable = false, columnDefinition = "text")
    private String phase;

    //userId of the discord user; there might be special negative values for factional actors/targets in the future
    @Column(name = "actor", nullable = false)
    private long actor;

    //defined in the Actions enum
    @Column(name = "action_type", nullable = false, columnDefinition = "text")
    private String actionType;

    //userId of the discord user
    @Column(name = "target", nullable = false)
    private long target;

    //save any additional info of an action in here
    @Column(name = "additional_info", columnDefinition = "text", nullable = true)
    private String additionalInfo;

    public ActionStats(final GameStats game, final int order, final long timeStampSubmitted,
                       final long timeStampHappened, final int cycle, final Phase phase, final long actor,
                       final Actions action, final long target, @Nullable final String additionalInfo) {
        this.game = game;
        this.order = order;
        this.timeStampSubmitted = timeStampSubmitted;
        this.timeStampHappened = timeStampHappened;
        this.cycle = cycle;
        this.phase = phase.name();
        this.actor = actor;
        this.actionType = action.name();
        this.target = target;
        this.additionalInfo = additionalInfo;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = this.game.hashCode();
        result = prime * result + this.order;
        result = prime * result + (int) (this.timeStampSubmitted ^ (this.timeStampSubmitted >>> 32));
        result = prime * result + (int) (this.timeStampHappened ^ (this.timeStampHappened >>> 32));
        result = prime * result + (int) (this.actor ^ (this.actor >>> 32));
        result = prime * result + this.actionType.hashCode();
        result = prime * result + (int) (this.target ^ (this.target >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ActionStats)) {
            return false;
        }
        final ActionStats a = (ActionStats) obj;
        return this.game.equals(a.game)
                && this.order == a.order
                && this.timeStampSubmitted == a.timeStampSubmitted
                && this.timeStampHappened == a.timeStampHappened
                && this.actor == a.actor
                && this.actionType.equals(a.actionType)
                && this.target == a.target;
    }

    //how much time since game started
    private String gameTime() {
        return TextchatUtils.formatMillis(getTimeStampHappened() - this.game.getStartTime());
    }

    //giant ass method of displaying an action
    @Override
    public String toString() {

        String result = "`" + gameTime() + "` ";
        switch (Actions.valueOf(this.actionType)) {

            case GAMESTART:
                result += String.format("%s: Game **#%s** starts.", Emojis.VIDEO_GAME, getGame().getId());
                break;
            case GAMEEND:
                result += String.format("%s: Game **#%s** ends.", Emojis.END, getGame().getId());
                break;
            case DAYSTART:
                result += String.format("%s: Day **%s** starts.", Emojis.SUNNY, getCycle());
                break;
            case DAYEND:
                result += String.format("%s: Day **%s** ends.", Emojis.CITY_SUNSET_SUNRISE, getCycle());
                break;
            case NIGHTSTART:
                result += String.format("%s: Night **%s** starts.", Emojis.FULL_MOON, getCycle());
                break;
            case NIGHTEND:
                result += String.format("%s: Night **%s** ends.", Emojis.CITY_SUNSET_SUNRISE, getCycle());
                break;
            case BOTKILL:
                result += String.format("%s: %s botkilled.", Emojis.SKULL, getFormattedNickFromStats(this.target));
                break;
            case MODKILL:
                result += String.format("%s: %s modkilled.", Emojis.COFFIN, getFormattedNickFromStats(this.target));
                break;
            case DEATH:
                result += String.format("%s: %s dies.", Emojis.RIP, getFormattedNickFromStats(this.target));
                break;
            case VOTELYNCH:
                result += String.format("%s: %s votes to lynch %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target));
                break;
            case LYNCH:
                result += String.format("%s: %s is lynched", Emojis.FIRE, getFormattedNickFromStats(this.target));
                break;
            case VOTENIGHTKILL:
                result += String.format("%s: %s votes to night kill %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target));
                break;
            case CHECK:
                result += String.format("%s: %s checks alignment of %s.", Emojis.MAGNIFIER, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target));
                break;
            case SHOOT:
                result += String.format("%s: %s shoots %s.", Emojis.GUN, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target));
                break;
            case VOTEGUN:
                result += String.format("%s: %s votes to give %s the %s.", Emojis.BALLOT_BOX, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target), Emojis.GUN);
                break;
            case GIVEGUN:
                result += String.format("%s: %s receives the gun", Emojis.GUN, getFormattedNickFromStats(this.target));
                break;
            case GIVE_PRESENT:
                result += String.format("%s: %s gives %s a present", Item.Items.PRESENT, getFormattedNickFromStats(this.actor), getFormattedNickFromStats(this.target));
                break;
            case OPEN_PRESENT:
                result += String.format("%s: %s opens a present and receives a %s", Item.Items.PRESENT, getFormattedNickFromStats(this.target), Item.Items.valueOf(this.additionalInfo));
                break;
            default:
                throw new IllegalArgumentException("Encountered an action that is not defined/has no text representation: " + this.actionType);
        }

        return result;
    }

    private String getFormattedNickFromStats(final long userId) {
        String baddieEmoji = Emojis.SPY;
        if (this.game.getGameType() == Games.POPCORN) baddieEmoji = Emojis.WOLF;
        for (final TeamStats team : this.game.getStartingTeams()) {
            for (final PlayerStats player : team.getPlayers()) {
                if (player.getUserId() == userId)
                    return "`" + player.getNickname() + "` " + (player.getAlignment() == Alignments.VILLAGE ? Emojis.COWBOY : baddieEmoji);
            }
        }
        final String message = String.format("No such player %s in this game %s", userId, this.game.getId());
        log.error(message, new IllegalArgumentException(message));
        return DiscordUser.UNKNOWN_NAME;
    }

    //########## boilerplate code below

    protected ActionStats() {
    }

    @Override
    @Nonnull
    public Long getId() {
        return this.id;
    }

    @Nonnull
    @Override
    public ActionStats setId(final Long id) {
        this.id = id;
        return this;
    }

    public GameStats getGame() {
        return this.game;
    }

    public void setGame(final GameStats game) {
        this.game = game;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    public long getTimeStampSubmitted() {
        return this.timeStampSubmitted;
    }

    public void setTimeStampSubmitted(final long timeStampSubmitted) {
        this.timeStampSubmitted = timeStampSubmitted;
    }

    public long getTimeStampHappened() {
        return this.timeStampHappened;
    }

    public void setTimeStampHappened(final long timeStampHappened) {
        this.timeStampHappened = timeStampHappened;
    }

    public int getCycle() {
        return this.cycle;
    }

    public void setCycle(final int cycle) {
        this.cycle = cycle;
    }

    public Phase getPhase() {
        return Phase.valueOf(this.phase);
    }

    public void setPhase(final Phase phase) {
        this.phase = phase.name();
    }

    public long getActor() {
        return this.actor;
    }

    public void setActor(final long actor) {
        this.actor = actor;
    }

    public Actions getActionType() {
        return Actions.valueOf(this.actionType);
    }

    public void setActionType(final Actions actionType) {
        this.actionType = actionType.name();
    }

    public long getTarget() {
        return this.target;
    }

    public void setTarget(final long target) {
        this.target = target;
    }

    @Nullable
    public String getAdditionalInfo() {
        return this.additionalInfo;
    }

    /**
     * @return itself for chaining
     */
    public ActionStats setAdditionalInfo(@Nullable final String additionalInfo) {
        this.additionalInfo = additionalInfo;
        return this;
    }
}
