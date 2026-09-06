package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySlug(String slug);

    boolean existsBySlug(String slug);
}