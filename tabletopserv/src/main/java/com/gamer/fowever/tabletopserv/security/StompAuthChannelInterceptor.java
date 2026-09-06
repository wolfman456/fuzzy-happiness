package com.gamer.fowever.tabletopserv.security;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SESSION_DESTINATION =
            Pattern.compile("^/(topic|app)/sessions/(\\d+)(/.*)?$");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    public StompAuthChannelInterceptor(JwtService jwtService,
                                       UserRepository userRepository,
                                       ParticipantRepository participantRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            User user = resolveUser(accessor);
            if (user == null) {
                throw new MessageDeliveryException("Authentication required");
            }
            if (command == StompCommand.CONNECT) {
                return message;
            }
            Long sessionId = sessionIdFor(accessor.getDestination());
            if (sessionId != null && !participantRepository.existsBySessionIdAndUserId(sessionId, user.getId())) {
                throw new MessageDeliveryException("You are not a participant of this session");
            }
            if (sessionId == null && command == StompCommand.SUBSCRIBE
                    && !isUserQueue(accessor.getDestination())) {
                throw new MessageDeliveryException("Unsupported subscription destination");
            }
        }
        return message;
    }

    private User resolveUser(StompHeaderAccessor accessor) {
        Object principal = accessor.getUser();
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        String token = bearerTokenFromHeaders(accessor);
        if (token == null) {
            token = tokenFromHeaders(accessor);
        }
        if (token == null) {
            return null;
        }
        try {
            Long userId = jwtService.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && jwtService.isTokenValid(token, user)) {
                accessor.setUser(new AuthenticatedUser(user));
                return user;
            }
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private String bearerTokenFromHeaders(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        return value != null && value.startsWith("Bearer ") ? value.substring(7).trim() : value;
    }

    private String tokenFromHeaders(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("token");
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private Long sessionIdFor(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = SESSION_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        return Long.valueOf(matcher.group(2));
    }

    private boolean isUserQueue(String destination) {
        return destination != null && destination.startsWith("/user/queue/");
    }
}