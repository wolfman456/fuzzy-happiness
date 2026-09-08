package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.BattleMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BattleMapRepository extends JpaRepository<BattleMap, Long> {

    Optional<BattleMap> findBySessionId(Long sessionId);
}