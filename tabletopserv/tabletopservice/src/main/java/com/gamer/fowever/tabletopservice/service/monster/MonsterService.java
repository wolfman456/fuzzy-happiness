package com.gamer.fowever.tabletopservice.service.monster;

import com.gamer.fowever.tabletopapi.MonsterRole;
import com.gamer.fowever.tabletopapi.dto.GenerateMonsterRequest;
import com.gamer.fowever.tabletopapi.dto.MonsterDto;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.Monster;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.MonsterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

@Service
public class MonsterService {

    private final MonsterRepository monsterRepository;
    private final ObjectMapper objectMapper;

    public MonsterService(MonsterRepository monsterRepository, ObjectMapper objectMapper) {
        this.monsterRepository = monsterRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MonsterDto generate(User actor, GenerateMonsterRequest request) {
        String cr = request.cr().trim();
        if (MonsterMathEngine.baselineFor(cr).isEmpty()) {
            throw ApiException.badRequest("unsupported challenge rating: " + cr
                    + " (allowed: " + MonsterMathEngine.challengeRatings() + ")");
        }
        MonsterRole role = request.roleOrAuto() == MonsterRole.AUTO
                ? MonsterMathEngine.resolveRole(request.concept(), request.name())
                : request.roleOrAuto();
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                request.name(), cr, role, request.editionOrDefault());
        Monster saved = monsterRepository.save(new Monster(
                actor, statblock.name(), cr, role, request.editionOrDefault(),
                toJson(statblock), Instant.now()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MonsterDto> mine(User actor) {
        return monsterRepository.findByOwnerIdOrderByCreatedAtDesc(actor.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    private MonsterDto toDto(Monster monster) {
        MonsterMathEngine.Statblock statblock = fromJson(monster.getStatblockJson());
        return new MonsterDto(
                monster.getId(), statblock.name(), statblock.crLabel(), statblock.xp(),
                statblock.edition(), statblock.role(), statblock.proficiencyBonus(),
                statblock.armorClass(), statblock.hitPoints(), statblock.size(), statblock.type(),
                statblock.alignment(), statblock.speedFeet(), statblock.strength(),
                statblock.dexterity(), statblock.constitution(), statblock.intelligence(),
                statblock.wisdom(), statblock.charisma(), statblock.attackBonus(),
                statblock.saveDc(), statblock.damagePerRound(), statblock.description(),
                statblock.actions().stream()
                        .map(action -> new MonsterDto.MonsterAction(action.name(), action.description()))
                        .toList(),
                statblock.traits(), monster.getCreatedAt());
    }

    private String toJson(MonsterMathEngine.Statblock statblock) {
        try {
            return objectMapper.writeValueAsString(statblock);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("monster statblock serialization failed");
        }
    }

    private MonsterMathEngine.Statblock fromJson(String json) {
        try {
            return objectMapper.readValue(json, MonsterMathEngine.Statblock.class);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("monster statblock parse failed");
        }
    }
}