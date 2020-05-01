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

package space.npstr.wolfia.commands.util;


import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Role;
import space.npstr.wolfia.App;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.GuildCommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.domain.Command;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.utils.discord.TextchatUtils;

/**
 * Created by napster on 07.01.18.
 * <p>
 * Allows users to add / remove special roles in the Wolfia Lounge
 */
@Command
public class RankCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "rank";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RankCommand.class);

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public boolean execute(@Nonnull final CommandContext commandContext) {
        final GuildCommandContext context = commandContext.requireGuild();
        if (context == null) {
            return false;
        }

        if (context.guild.getIdLong() != App.WOLFIA_LOUNGE_ID) {
            context.reply(String.format("This command is restricted to the official Wolfia Lounge. Say `%s` to get invited.",
                    WolfiaConfig.DEFAULT_PREFIX + InviteCommand.TRIGGER));
            return false;
        }

        if (!context.hasArguments()) {
            context.help();
            return false;
        }

        final Role role;
        if (TextchatUtils.isSimilarLower("AlphaWolves", context.rawArgs)) {
            role = context.guild.getRoleById(WolfiaGuildListener.ALPHAWOLVES_ROLE_ID);
            if (role == null) {
                log.warn("Did the AlphaWolves role disappear in the Wolfia Lounge?");
                return false;
            }
        } else {
            context.help();
            return false;
        }

        if (context.member.getRoles().contains(role)) {
            context.guild.removeRoleFromMember(context.member, role).queue();
            context.replyWithName(String.format("removed role `%s` from you.", role.getName()));
        } else {
            context.guild.addRoleToMember(context.member, role).queue();
            context.replyWithName(String.format("added role `%s` to you.", role.getName()));
        }
        return true;
    }

    @Nonnull
    @Override
    public String help() {
        return invocation() + " AlphaWolves"
                + "\n#Add or remove special roles of the official Wolfia Lounge for you.";
    }
}
