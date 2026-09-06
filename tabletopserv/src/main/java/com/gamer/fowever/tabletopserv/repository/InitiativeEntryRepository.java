package com.gamer.fowever.tabletopserv.repository;

import com.gamer.fowever.tabletopserv.domain.InitiativeEntry;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;
import java.util.Optional;

public interface InitiativeEntryRepository extends ListCrudRepository<InitiativeEntry, Long> {

    List<InitiativeEntry> findByBattleMapIdOrderByScoreDescIdAsc(Long battleMapId);

    Optional<InitiativeEntry> findByIdAndBattleMapId(Long id, Long battleMapId);
}