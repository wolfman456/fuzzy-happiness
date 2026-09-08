package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.MapToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MapTokenRepository extends JpaRepository<MapToken, Long> {

    List<MapToken> findByMapIdOrderByIdAsc(Long mapId);

    Optional<MapToken> findByIdAndMapId(Long id, Long mapId);
}