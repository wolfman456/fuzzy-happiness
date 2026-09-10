package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    List<Character> findByOwnerIdOrderByIdDesc(Long ownerId);

    Optional<Character> findByIdAndOwnerId(Long id, Long ownerId);
}