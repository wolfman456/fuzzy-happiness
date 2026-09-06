package com.gamer.fowever.tabletopserv.security;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Component
public class TokenHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TokenHandshakeHandler(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String token = bearerTokenFromHeader(request);
        if (token == null) {
            token = tokenFromQuery(request);
        }
        if (token == null) {
            return null;
        }
        try {
            Long userId = jwtService.extractUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !jwtService.isTokenValid(token, user)) {
                return null;
            }
            return new AuthenticatedUser(user);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    private String bearerTokenFromHeader(ServerHttpRequest request) {
        List<String> authorization = request.getHeaders().get("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            return null;
        }
        String value = authorization.get(0);
        if (value != null && value.startsWith("Bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }

    private String tokenFromQuery(ServerHttpRequest request) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        return params.getFirst("token");
    }
}