package com.gamer.fowever.tabletopservice.repository;

import com.gamer.fowever.tabletopservice.domain.Monster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonsterRepository extends JpaRepository<Monster, Long> {

    List<Monster> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}