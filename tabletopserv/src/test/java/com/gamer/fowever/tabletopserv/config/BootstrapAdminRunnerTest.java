package com.gamer.fowever.tabletopserv.config;

import com.gamer.fowever.tabletopserv.domain.AuthRole;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private BootstrapAdminRunner runner() {
        return new BootstrapAdminRunner(userRepository, passwordEncoder,
                "admin", "admin@tabletop.local", "AdminPassw0rd!");
    }

    @Test
    void createsVerifiedAdminWhenMissing() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@tabletop.local")).thenReturn(false);
        when(passwordEncoder.encode("AdminPassw0rd!")).thenReturn("admin-hash");

        runner().run();

        verify(userRepository).save(argThat(user -> {
            assertThat(user.getAuthRole()).isEqualTo(AuthRole.ADMIN);
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getPasswordHash()).isEqualTo("admin-hash");
            assertThat(user.getUsername()).isEqualTo("admin");
            return true;
        }));
    }

    @Test
    void skipsWhenAdminExists() {
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        runner().run();

        verify(userRepository, never()).save(any());
    }
}