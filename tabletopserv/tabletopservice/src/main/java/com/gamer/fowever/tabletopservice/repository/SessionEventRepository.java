package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    List<SessionEvent> findTop50BySessionIdOrderByIdDesc(Long sessionId);
}