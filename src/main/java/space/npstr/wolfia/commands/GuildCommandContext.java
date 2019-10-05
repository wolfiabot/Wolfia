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

package space.npstr.wolfia.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;

import javax.annotation.Nonnull;

/**
 * Created by napster on 09.12.17.
 * <p>
 * Provides @Nonnull methods for accessing guild entities after an elegant transformation from a CommandContext
 * Same rules as for the CommandContext, don't save these or hold on to these for an extended period of time as it
 * holds direct references to the entities.
 */
public class GuildCommandContext extends CommandContext {

    @Nonnull
    public final Guild guild;
    @Nonnull
    public final Member member;
    @Nonnull
    public final TextChannel textChannel;


    @Nonnull
    @Override
    public Guild getGuild() {
        return this.guild;
    }

    @Nonnull
    @Override
    public Member getMember() {
        return this.member;
    }

    @Nonnull
    public TextChannel getTextChannel() {
        return this.textChannel;
    }

    public GuildCommandContext(@Nonnull final CommandContext context, @Nonnull final Guild guild,
                               @Nonnull final Member member, @Nonnull final TextChannel textChannel) {
        super(context.event, context.trigger, context.args, context.rawArgs, context.command);
        this.guild = guild;
        this.member = member;
        this.textChannel = textChannel;
    }
}
