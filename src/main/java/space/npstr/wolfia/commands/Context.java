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


import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @Nullable
    @CheckReturnValue
    Message getMessage();

    @Nullable
    @CheckReturnValue
    MessageReceivedEvent getEvent();

    @Nullable
    @CheckReturnValue
    JDA getJda();

    @Nullable
    @CheckReturnValue
    Guild getGuild();

    /**
     * @return Member entity of the invoker
     */
    @Nullable
    @CheckReturnValue
    Member getMember();
}
