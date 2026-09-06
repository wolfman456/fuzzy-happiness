package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.BattleMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BattleMapRepository extends JpaRepository<BattleMap, Long> {

    Optional<BattleMap> findBySessionId(Long sessionId);
}