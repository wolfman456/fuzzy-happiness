package com.gamer.fowever.tabletopserv.security;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    private static final Long SESSION_ID = 7L;

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParticipantRepository participantRepository;

    private StompAuthChannelInterceptor interceptor;
    private User user;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtService, userRepository, participantRepository);
        user = new User("aria", "Aria", "aria@example.com", LocalDate.of(1990, 1, 1), "hash");
        user.setId(1L);
        user.setEmailVerified(true);
    }

    @Test
    void connectWithHandshakePrincipalPasses() {
        Message<?> message = message(StompCommand.CONNECT, null, principal(), null, null);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void connectWithoutPrincipalIsRejected() {
        Message<?> message = message(StompCommand.CONNECT, null, null, null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void memberCanSubscribeToSessionTopic() {
        when(participantRepository.existsBySessionIdAndUserId(SESSION_ID, user.getId())).thenReturn(true);
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/sessions/7", principal(), null, null);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void nonMemberCannotSubscribeToSessionTopic() {
        when(participantRepository.existsBySessionIdAndUserId(SESSION_ID, user.getId())).thenReturn(false);
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/sessions/7", principal(), null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void memberCanSubscribeToApplicationSnapshotDestination() {
        when(participantRepository.existsBySessionIdAndUserId(SESSION_ID, user.getId())).thenReturn(true);
        Message<?> message = message(StompCommand.SUBSCRIBE, "/app/sessions/7", principal(), null, null);

        interceptor.preSend(message, mock(MessageChannel.class));
    }

    @Test
    void memberCanSendChatMessage() {
        when(participantRepository.existsBySessionIdAndUserId(SESSION_ID, user.getId())).thenReturn(true);
        Message<?> message = message(StompCommand.SEND, "/app/sessions/7/chat", principal(), null, null);

        interceptor.preSend(message, mock(MessageChannel.class));
    }

    @Test
    void subscribeToNonSessionDestinationIsRejected() {
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/other", principal(), null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void sendToNonSessionDestinationPassesThrough() {
        Message<?> message = message(StompCommand.SEND, "/app/elsewhere", principal(), null, null);

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void unauthenticatedSenderIsRejected() {
        Message<?> message = message(StompCommand.SEND, "/app/sessions/7/chat", null, null, null);

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
        verify(participantRepository, never()).existsBySessionIdAndUserId(SESSION_ID, user.getId());
    }

    @Test
    void reconnectAuthenticatesFromAuthorizationNativeHeader() {
        when(jwtService.extractUserId("the-token")).thenReturn(user.getId());
        when(jwtService.isTokenValid("the-token", user)).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(java.util.Optional.of(user));
        when(participantRepository.existsBySessionIdAndUserId(SESSION_ID, user.getId())).thenReturn(true);

        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/sessions/7", null,
                "Authorization", "Bearer the-token");

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));

        assertThat(result).isSameAs(message);
    }

    @Test
    void invalidNativeHeaderTokenIsRejected() {
        when(jwtService.extractUserId("bad-token")).thenThrow(new io.jsonwebtoken.JwtException("bad"));
        Message<?> message = message(StompCommand.SUBSCRIBE, "/topic/sessions/7", null, "token", "bad-token");

        assertThatThrownBy(() -> interceptor.preSend(message, mock(MessageChannel.class)))
                .isInstanceOf(MessageDeliveryException.class);
    }

    private Message<?> message(StompCommand command, String destination, Object principal,
                               String nativeHeaderName, String nativeHeaderValue) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (principal != null) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, user.getAuthorities()));
        }
        if (nativeHeaderName != null) {
            accessor.addNativeHeader(nativeHeaderName, nativeHeaderValue);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Object principal() {
        return user;
    }
}