package com.gamer.fowever.tabletopserv.service;

import com.gamer.fowever.tabletopserv.domain.AuthRole;
import com.gamer.fowever.tabletopserv.domain.EmailVerificationToken;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.AuthResponse;
import com.gamer.fowever.tabletopserv.dto.LoginRequest;
import com.gamer.fowever.tabletopserv.dto.RegisterRequest;
import com.gamer.fowever.tabletopserv.dto.RegisterResponse;
import com.gamer.fowever.tabletopserv.email.EmailSender;
import com.gamer.fowever.tabletopserv.repository.EmailVerificationTokenRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.gamer.fowever.tabletopserv.security.JwtService;
import com.gamer.fowever.tabletopserv.support.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final long TTL_MILLIS = 86_400_000;
    private static final long COOLDOWN_MILLIS = 60_000;
    private static final long JWT_MILLIS = 86_400_000;

    private static final RegisterRequest REGISTER = new RegisterRequest(
            "Aria", "aria@example.com", LocalDate.of(1990, 1, 15), "aria", "Password1!");

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailSender emailSender;
    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, tokenRepository, passwordEncoder, jwtService,
                emailSender, authenticationManager, BASE_URL, TTL_MILLIS, COOLDOWN_MILLIS, JWT_MILLIS);
    }

    private User user(boolean emailVerified) {
        User user = new User("aria", "Aria", "aria@example.com",
                LocalDate.of(1990, 1, 15), "hash-value");
        user.setId(1L);
        user.setEmailVerified(emailVerified);
        return user;
    }

    @Test
    void registersUserAsUnverifiedAndSendsVerificationEmail() {
        when(userRepository.existsByUsernameIgnoreCase("aria")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("aria@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-hash");
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(1L);
            }
            return saved;
        });

        RegisterResponse response = service.register(REGISTER);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.message()).contains("verify");
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.getAuthRole() == AuthRole.USER && !user.isEmailVerified()
                        && user.getPasswordHash().equals("encoded-hash")
                        && user.getUsername().equals("aria")));

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).hasSize(64);
        assertThat(tokenCaptor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());

        verify(emailSender).sendVerificationEmail(eq("aria@example.com"),
                contains("/api/auth/verify?token=" + tokenCaptor.getValue().getToken()));
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("aria")).thenReturn(true);

        assertThatThrownBy(() -> service.register(REGISTER))
                .isInstanceOf(ApiException.class)
                .hasMessage("Username is already taken");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByUsernameIgnoreCase("aria")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("aria@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(REGISTER))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejectsUnderageRegistration() {
        RegisterRequest underage = new RegisterRequest("Kid", "kid@example.com",
                LocalDate.of(2015, 1, 1), "kid", "Password1!");

        assertThatThrownBy(() -> service.register(underage))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("13");
    }

    @Test
    void logsInVerifiedUserWithToken() {
        User user = user(true);
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = service.login(new LoginRequest("aria", "Password1!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(86_400);
        assertThat(response.user().username()).isEqualTo("aria");
        assertThat(response.user().email()).isEqualTo("aria@example.com");
    }

    @Test
    void rejectsUnverifiedLogin() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> service.login(new LoginRequest("aria", "Password1!")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("verified");
    }

    @Test
    void rejectsWrongPassword() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user(true)));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad password"));

        assertThatThrownBy(() -> service.login(new LoginRequest("aria", "Wrong!")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void rejectsUnknownLoginIdentifier() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("ghost", "ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ghost", "Password1!")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void verifiesEmailToken() {
        User user = user(false);
        EmailVerificationToken token = new EmailVerificationToken("tok", user, LocalDateTime.now().plusDays(1));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        service.verifyEmail("tok");

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(user.isEmailVerified()).isTrue();
        verify(tokenRepository).deleteByUser(user);
    }

    @Test
    void rejectsBlankVerificationToken() {
        assertThatThrownBy(() -> service.verifyEmail("   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("token");
    }

    @Test
    void rejectsUnknownVerificationToken() {
        when(tokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("nope"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid or expired");
    }

    @Test
    void rejectsUsedVerificationToken() {
        User user = user(false);
        EmailVerificationToken token = new EmailVerificationToken("tok", user, LocalDateTime.now().plusDays(1));
        token.setUsedAt(LocalDateTime.now());
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("tok"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("used");
    }

    @Test
    void rejectsExpiredVerificationToken() {
        User user = user(false);
        EmailVerificationToken token = new EmailVerificationToken("tok", user, LocalDateTime.now().minusMinutes(1));
        when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("tok"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resendDoesNothingForUnknownUser() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("ghost", "ghost")).thenReturn(Optional.empty());

        service.resendVerification("ghost");

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void resendSkipsVerifiedUser() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user(true)));

        service.resendVerification("aria");

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendVerificationEmail(any(), any());
    }

    @Test
    void resendRespectsCooldown() {
        User user = user(false);
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserAndCreatedAtAfter(eq(user), any())).thenReturn(true);

        assertThatThrownBy(() -> service.resendVerification("aria"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("wait");
    }

    @Test
    void resendReplacesOldTokenAndSendsNewLink() {
        User user = user(false);
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("aria", "aria")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserAndCreatedAtAfter(eq(user), any())).thenReturn(false);
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.resendVerification("aria");

        verify(tokenRepository).deleteByUser(user);
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).hasSize(64);
        verify(emailSender).sendVerificationEmail(eq("aria@example.com"), contains(captor.getValue().getToken()));
    }
}