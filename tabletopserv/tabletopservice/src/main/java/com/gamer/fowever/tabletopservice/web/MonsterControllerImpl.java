package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.MonsterApi;
import com.gamer.fowever.tabletopapi.dto.GenerateMonsterRequest;
import com.gamer.fowever.tabletopapi.dto.MonsterDto;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.monster.MonsterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monsters")
public class MonsterControllerImpl implements MonsterApi {

    private final MonsterService monsterService;

    public MonsterControllerImpl(MonsterService monsterService) {
        this.monsterService = monsterService;
    }

    @Override
    public ResponseEntity<MonsterDto> generate(@Valid @RequestBody GenerateMonsterRequest request,
                                               Authentication authentication) {
        MonsterDto monster = monsterService.generate((User) authentication.getPrincipal(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(monster);
    }

    @Override
    public List<MonsterDto> mine(Authentication authentication) {
        return monsterService.mine((User) authentication.getPrincipal());
    }
}