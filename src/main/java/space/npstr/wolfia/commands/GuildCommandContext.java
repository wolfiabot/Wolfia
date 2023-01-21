/*
 * Copyright (C) 2016-2020 the original author or authors
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

import java.util.Optional;
import org.springframework.lang.NonNull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Provides @NonNull methods for accessing guild entities after an elegant transformation from a CommandContext
 * Same rules as for the CommandContext, don't save these or hold on to these for an extended period of time as it
 * holds direct references to the entities.
 */
public class GuildCommandContext extends CommandContext {

    @NonNull
    public final Guild guild;
    @NonNull
    public final Member member;
    @NonNull
    public final TextChannel textChannel;


    @Override
    public Optional<Guild> getGuild() {
        return Optional.of(this.guild);
    }

    @Override
    public Optional<Member> getMember() {
        return Optional.of(this.member);
    }

    @NonNull
    public TextChannel getTextChannel() {
        return this.textChannel;
    }

    public GuildCommandContext(@NonNull final CommandContext context, @NonNull final Guild guild,
                               @NonNull final Member member, @NonNull final TextChannel textChannel) {
        super(context.event, context.trigger, context.args, context.rawArgs, context.command);
        this.guild = guild;
        this.member = member;
        this.textChannel = textChannel;
    }
}
