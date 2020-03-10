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


import java.util.Optional;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods
 */
@SuppressWarnings("unused")
public interface Context {

    @Nonnull
    @CheckReturnValue
    MessageChannel getChannel();

    @Nonnull
    @CheckReturnValue
    User getInvoker();

    //message that triggered this context
    @Nonnull
    @CheckReturnValue
    Message getMessage();

    @Nonnull
    @CheckReturnValue
    MessageReceivedEvent getEvent();

    @Nonnull
    @CheckReturnValue
    JDA getJda();

    @CheckReturnValue
    Optional<Guild> getGuild();

    /**
     * @return Member entity of the invoker
     */
    @CheckReturnValue
    Optional<Member> getMember();
}
