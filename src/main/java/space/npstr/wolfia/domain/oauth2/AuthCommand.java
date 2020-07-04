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

package space.npstr.wolfia.domain.oauth2;

import javax.annotation.Nonnull;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.PublicCommand;
import space.npstr.wolfia.domain.Command;

@Command
public class AuthCommand implements BaseCommand, PublicCommand {

    public static final String TRIGGER = "auth";


    private final AuthStateCache stateCache;
    private final OAuth2Requester requester;

    public AuthCommand(AuthStateCache stateCache, OAuth2Requester requester) {
        this.stateCache = stateCache;
        this.requester = requester;
    }

    @Override
    public String getTrigger() {
        return TRIGGER;
    }

    @Override
    public boolean execute(@Nonnull CommandContext context) {

        String jumpUrl = context.getMessage().getJumpUrl();

        AuthState authState = ImmutableAuthState.builder()
                .userId(context.getInvoker().getIdLong())
                .redirectUrl(jumpUrl)
                .build();

        String state = this.stateCache.generateStateParam(authState);
        String privateMessage = "Click the following link to connect with Wolfia. This will allow me to automatically "
                + "add you to wolf chat or global games.\n" + this.requester.getAuthorizationUrl(state);

        context.replyPrivate(privateMessage, __ -> context.replyWithMention("check your direct messages!"),
                t -> context.replyWithMention("I cannot send you a private message, please unblock me and/or adjust your discord privacy settings."));
        return true;
    }

    @Nonnull
    @Override
    public String help() {
        return "Start the authorization flow with Wolfia. Once you have successfully authorized with me, "
                + "I will be able to automatically add you to wolf chat or global games.";
    }
}
