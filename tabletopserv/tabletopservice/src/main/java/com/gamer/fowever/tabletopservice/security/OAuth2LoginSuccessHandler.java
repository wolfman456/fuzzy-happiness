package com.gamer.fowever.tabletopservice.security;

import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * After a successful provider handshake, mints the same JWT as password login and redirects the
 * browser to the SPA callback route. The token travels in the URL fragment (never sent to a
 * server, kept out of logs/referrers).
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final long jwtExpirationMillis;

    public OAuth2LoginSuccessHandler(JwtService jwtService,
                                     UserRepository userRepository,
                                     @Value("${tabletopserv.app.frontend-url}") String frontendUrl,
                                     @Value("${tabletopserv.jwt.expiration-millis}") long jwtExpirationMillis) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.jwtExpirationMillis = jwtExpirationMillis;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AppOAuth2User principal = (AppOAuth2User) token.getPrincipal();
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new IllegalStateException("Social user vanished after login"));
        String jwt = jwtService.generateToken(user);
        String target = frontendUrl + "/oauth/callback#token=" + encode(jwt)
                + "&tokenType=Bearer&expiresInSeconds=" + (jwtExpirationMillis / 1000);
        response.sendRedirect(target);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
