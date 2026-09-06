package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.SessionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionEventRepository extends JpaRepository<SessionEvent, Long> {

    List<SessionEvent> findTop50BySessionIdOrderByIdDesc(Long sessionId);
}