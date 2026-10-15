package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.BattleMapTile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleMapTileRepository extends JpaRepository<BattleMapTile, Long> {

    List<BattleMapTile> findByMapIdOrderByPosYAscPosXAsc(Long mapId);

    Optional<BattleMapTile> findByMapIdAndPosXAndPosY(Long mapId, int posX, int posY);

    List<BattleMapTile> findByMapId(Long mapId);
}
