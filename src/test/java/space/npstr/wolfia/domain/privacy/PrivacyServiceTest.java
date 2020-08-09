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

package space.npstr.wolfia.domain.privacy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import space.npstr.wolfia.ApplicationTest;
import space.npstr.wolfia.commands.CommandHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static space.npstr.wolfia.TestUtil.uniqueLong;

class PrivacyServiceTest<T extends Session> extends ApplicationTest {

    @Autowired
    private PrivacyService privacyService;

    @Autowired
    private SessionRepository<T> sessionRepository;

    @Autowired
    private CommandHandler commandHandler;

    /**
     * See {@link space.npstr.wolfia.domain.stats.StatsServiceTest} for a more complete test of this
     */
    @Test
    void whenDataDelete_gameStatsOfUserAreAnonymized() {
        long userId = uniqueLong();

        this.privacyService.dataDelete(userId);

        verify(statsService).anonymize(eq(userId));
    }

    @Test
    void afterDataDelete_commandsByUserAreIgnored() {
        long userId = uniqueLong();
        User user = mock(User.class);
        when(user.getIdLong()).thenReturn(userId);
        MessageReceivedEvent messageReceived = mock(MessageReceivedEvent.class);
        when(messageReceived.getAuthor()).thenReturn(user);

        Message message = mock(Message.class);
        when(message.getContentRaw()).thenReturn("w.privacy");
        when(messageReceived.getMessage()).thenReturn(message);

        MessageChannel channel = mock(MessageChannel.class);
        when(message.getIdLong()).thenReturn(uniqueLong());
        when(messageReceived.getChannel()).thenReturn(channel);

        JDA jda = mock(JDA.class);
        when(jda.getShardManager()).thenReturn(shardManager);
        when(messageReceived.getJDA()).thenReturn(jda);

        doAnswer(invocation -> true).when(privacyCommand).execute(any());

        this.commandHandler.onMessageReceived(messageReceived);
        verify(privacyCommand, times(1)).execute(any());
        this.commandHandler.onMessageReceived(messageReceived);
        verify(privacyCommand, times(2)).execute(any());

        this.privacyService.dataDelete(userId);

        this.commandHandler.onMessageReceived(messageReceived);
        verify(privacyCommand, times(2)).execute(any());
    }

    @Test
    void whenDataDelete_sessionsOfUserAreDeleted() {
        long userId = uniqueLong();
        T session = generateHttpSession(userId);
        assertThat(this.sessionRepository.findById(session.getId())).isNotNull();

        this.privacyService.dataDelete(userId);

        assertThat(this.sessionRepository.findById(session.getId())).isNull();
    }

    @Test
    @Disabled
    void afterDataDelete_userCantLogin() {
        // TODO how do we test this? our login logic is very much bound to Discords OAuth2 api and mocking it is not worht the effort
        throw new UnsupportedOperationException();
    }

    @Test
    @Disabled
    void whenDataDelete_bannedFromHomeGuild() {
        // TODO
        throw new UnsupportedOperationException();
    }


    private T generateHttpSession(long userId, final String... requestedAuthorities) {
        final Set<GrantedAuthority> authorities =
                Arrays.stream(requestedAuthorities)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toSet());

        final UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                Long.toString(userId),
                "bar",
                true,
                true,
                true,
                true,
                authorities
        );

        final Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, userDetails.getPassword(), userDetails.getAuthorities());

        final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, authentication.getCredentials(), userDetails.getAuthorities());
        authenticationToken.setDetails(authentication.getDetails());

        final SecurityContext securityContext = new SecurityContextImpl(authentication);

        T session = sessionRepository.createSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        session.setAttribute("sessionId", session.getId());
        sessionRepository.save(session);
        return session;
    }

}
