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
import javax.annotation.CheckReturnValue;
import org.springframework.lang.NonNull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods
 */
@SuppressWarnings("unused")
public interface Context {

    @NonNull
    @CheckReturnValue
    MessageChannel getChannel();

    @NonNull
    @CheckReturnValue
    User getInvoker();

    //message that triggered this context
    @NonNull
    @CheckReturnValue
    Message getMessage();

    @NonNull
    @CheckReturnValue
    MessageReceivedEvent getEvent();

    @NonNull
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
