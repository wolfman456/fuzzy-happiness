package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.AuthRole;
import com.gamer.fowever.tabletopapi.dto.AuthResponse;
import com.gamer.fowever.tabletopapi.dto.LoginRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterRequest;
import com.gamer.fowever.tabletopapi.dto.RegisterResponse;
import com.gamer.fowever.tabletopapi.dto.UpdateUsernameRequest;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopapi.support.PasswordPolicy;
import com.gamer.fowever.tabletopservice.domain.EmailVerificationToken;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.email.EmailSender;
import com.gamer.fowever.tabletopservice.repository.EmailVerificationTokenRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.security.JwtService;
import com.gamer.fowever.tabletopservice.support.Dtos;
import com.gamer.fowever.tabletopservice.support.PiiCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;
    private final AuthenticationManager authenticationManager;
    private final String baseUrl;
    private final long tokenTtlMillis;
    private final long resendCooldownMillis;
    private final long jwtExpirationMillis;

    public AuthService(UserRepository userRepository,
                       EmailVerificationTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailSender emailSender,
                       AuthenticationManager authenticationManager,
                       @Value("${tabletopserv.app.base-url}") String baseUrl,
                       @Value("${tabletopserv.verification.token-ttl-millis}") long tokenTtlMillis,
                       @Value("${tabletopserv.verification.resend-cooldown-millis}") long resendCooldownMillis,
                       @Value("${tabletopserv.jwt.expiration-millis}") long jwtExpirationMillis) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
        this.authenticationManager = authenticationManager;
        this.baseUrl = baseUrl;
        this.tokenTtlMillis = tokenTtlMillis;
        this.resendCooldownMillis = resendCooldownMillis;
        this.jwtExpirationMillis = jwtExpirationMillis;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw ApiException.badRequest("Passwords do not match");
        }
        enforceMinimumAge(request.dateOfBirth());
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw ApiException.conflict("Username is already taken");
        }
        if (userRepository.existsByEmailKey(PiiCrypto.emailKey(request.email()))) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = new User(request.username(), request.displayName(), request.email(),
                request.dateOfBirth(), passwordEncoder.encode(request.password()));
        user.setRealName(request.realName());
        user.setAuthRole(AuthRole.USER);
        user.setEmailVerified(false);
        userRepository.save(user);
        issueVerificationToken(user);
        return new RegisterResponse(user.getId(), "Account created. Check your email to verify your address.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameIgnoreCaseOrEmailKey(request.identifier(),
                        PiiCrypto.emailKey(request.identifier()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username/email or password"));
        if (!user.isEmailVerified()) {
            throw ApiException.forbidden("Email not verified. Check your inbox or request a new link.");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), request.password()));
        } catch (BadCredentialsException ex) {
            throw ApiException.unauthorized("Invalid username/email or password");
        }
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtExpirationMillis / 1000, Dtos.userSummary(user));
    }

    @Transactional
    public UserSummary updateUsername(User user, UpdateUsernameRequest request) {
        String newUsername = request.username().trim();
        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            return Dtos.userSummary(user);
        }
        if (userRepository.existsByUsernameIgnoreCase(newUsername)) {
            throw ApiException.conflict("Username is already taken");
        }
        user.setUsername(newUsername);
        userRepository.save(user);
        return Dtos.userSummary(user);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ApiException.badRequest("Verification token is required");
        }
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired verification link"));
        if (token.isUsed()) {
            throw ApiException.badRequest("This verification link has already been used");
        }
        if (token.isExpired()) {
            throw ApiException.badRequest("This verification link has expired");
        }
        token.setUsedAt(LocalDateTime.now());
        User user = token.getUser();
        user.setEmailVerified(true);
        tokenRepository.deleteByUser(user);
    }

    @Transactional
    public void resendVerification(String identifier) {
        userRepository.findByUsernameIgnoreCaseOrEmailKey(identifier, PiiCrypto.emailKey(identifier)).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }
            if (tokenRepository.existsByUserAndCreatedAtAfter(user, LocalDateTime.now().minus(Duration.ofMillis(resendCooldownMillis)))) {
                throw ApiException.tooManyRequests("Please wait a moment before requesting another link");
            }
            tokenRepository.deleteByUser(user);
            issueVerificationToken(user);
        });
    }

    private void issueVerificationToken(User user) {
        String tokenValue = generateTokenValue();
        EmailVerificationToken token = new EmailVerificationToken(tokenValue, user,
                LocalDateTime.now().plus(Duration.ofMillis(tokenTtlMillis)));
        tokenRepository.save(token);
        try {
            emailSender.sendVerificationEmail(user.getEmail(), baseUrl + "/api/auth/verify?token=" + tokenValue);
        } catch (RuntimeException ex) {
            log.warn("Verification email could not be delivered to user {} (id {}): the account is created but "
                    + "the link will not arrive until the mail server is configured. Token stays valid for "
                    + "/api/auth/verify and /api/auth/resend-verification. Cause: {}", user.getUsername(),
                    user.getId(), ex.getMessage());
        }
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void enforceMinimumAge(LocalDate dateOfBirth) {
        if (Period.between(dateOfBirth, LocalDate.now()).getYears() < PasswordPolicy.MIN_AGE) {
            throw ApiException.badRequest("You must be at least " + PasswordPolicy.MIN_AGE + " years old to register");
        }
    }
}