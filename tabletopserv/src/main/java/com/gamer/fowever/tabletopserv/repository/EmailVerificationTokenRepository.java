package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.EmailVerificationToken;
import com.gamer.fowever.tabletopserv.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUser(User user);

    boolean existsByUserAndCreatedAtAfter(User user, LocalDateTime createdAt);
}