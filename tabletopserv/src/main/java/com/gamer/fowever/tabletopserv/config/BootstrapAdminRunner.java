package com.gamer.fowever.tabletopserv.config;

import com.gamer.fowever.tabletopserv.domain.AuthRole;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@Profile("dev")
public class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public BootstrapAdminRunner(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${tabletopserv.admin.username}") String username,
                                @Value("${tabletopserv.admin.email}") String email,
                                @Value("${tabletopserv.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        User admin = new User(username, "Administrator", email, LocalDate.of(2000, 1, 1),
                passwordEncoder.encode(password));
        admin.setAuthRole(AuthRole.ADMIN);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        log.info("Bootstrap admin account created: {} / {}", username, email);
    }
}