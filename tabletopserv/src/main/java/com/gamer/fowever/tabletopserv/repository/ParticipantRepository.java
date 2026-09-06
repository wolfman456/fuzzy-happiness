package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findBySessionId(Long sessionId);

    Optional<Participant> findBySessionIdAndUserId(Long sessionId, Long userId);

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

    void deleteBySessionIdAndUserId(Long sessionId, Long userId);
}