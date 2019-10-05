/*
 * Copyright (C) 2016-2019 Dennis Neufeld
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

package space.npstr.wolfia.domain.ban;

import net.dv8tion.jda.core.entities.User;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.db.gen.tables.records.BanRecord;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by napster on 07.07.17.
 * <p>
 * Ban users from playing the game.
 */
@Command
public class BanCommand implements BaseCommand {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BanCommand.class);

    private final BanService banService;

    public BanCommand(BanService banService) {
        this.banService = banService;
    }

    @Override
    public String getTrigger() {
        return "ban";
    }

    @Nonnull
    @Override
    public String help() {
        return "Globally ban mentioned user from signing up for games.";
    }

    @Override
    public boolean execute(@Nonnull final CommandContext context) {

        //is the user allowed to do that?
        if (!context.isOwner()) {
            context.replyWithMention("you are not allowed to use the global banlist.");
            return false;
        }

        String option = "";
        if (context.hasArguments()) {
            option = context.args[0];
        }

        if (option.equalsIgnoreCase("list")) {
            final List<BanRecord> bans = this.banService.getActiveBans();
            String out = bans.stream()
                    .map(ban -> {
                        String result = ban.getUserId() + " " + TextchatUtils.userAsMention(ban.getUserId()) + " ";
                        try {
                            CachedUser user = CachedUser.load(ban.getUserId());
                            result += user.getEffectiveName(context.getGuild(),
                                    context.getJda().asBot().getShardManager()::getUserById);
                        } catch (DatabaseException e) {
                            log.error("Db exploded looking up a use of id {}", ban.getUserId(), e);
                        }
                        return result;
                    }).reduce("", (a, b) -> a + "\n" + b);
            if (out.isEmpty()) {
                out = "No global bans in effect!";
            } else {
                out = "List of global bans:\n" + out;
            }
            context.reply(out);
            return true;
        }

        BanAction action = null;
        if (TextchatUtils.isTrue(option)) {
            action = BanAction.ADD;
        } else if (TextchatUtils.isFalse(option)) {
            action = BanAction.REMOVE;
        }

        if (action == null) {
            final String answer = String.format("you didn't provide a ban action. Use `%s` or `%s`.",
                    TextchatUtils.TRUE_TEXT.get(0), TextchatUtils.FALSE_TEXT.get(0));
            context.replyWithMention(answer);
            return false;
        }

        if (context.args.length < 2) {
            context.reply("Please mention at least one user or use an id.");
            return false;
        }

        long userId;
        try {
            userId = Long.parseUnsignedLong(context.args[1]);
        } catch (final NumberFormatException ignored) {
            final List<User> mentionedUsers = context.msg.getMentionedUsers();
            if (mentionedUsers.isEmpty()) {
                context.reply("Please mention at least one user or use an id.");
                return false;
            } else {
                userId = mentionedUsers.get(0).getIdLong();
            }
        }

        if (action == BanAction.ADD) {
            this.banService.ban(userId);
            context.replyWithMention(String.format("added **%s** (%s) to the global ban list.",
                    CachedUser.load(userId).getEffectiveName(context.getGuild(),
                            context.getJda().asBot().getShardManager()::getUserById), TextchatUtils.userAsMention(userId)));
            return true;
        } else { //removing
            this.banService.unban(userId);
            context.replyWithMention(String.format("removed **%s** (%s) from the global ban list.",
                    CachedUser.load(userId).getEffectiveName(context.getGuild(),
                            context.getJda().asBot().getShardManager()::getUserById), TextchatUtils.userAsMention(userId)));
            return true;
        }
    }

    private enum BanAction {
        ADD,
        REMOVE
    }
}
