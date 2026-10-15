package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.BattleMapObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleMapObjectRepository extends JpaRepository<BattleMapObject, Long> {

    List<BattleMapObject> findByMapIdOrderByIdAsc(Long mapId);

    Optional<BattleMapObject> findByIdAndMapId(Long id, Long mapId);
}
