package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}