package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.GenerateMonsterRequest;
import com.gamer.fowever.tabletopapi.dto.MonsterDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/api/monsters")
public interface MonsterApi {

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MonsterDto> generate(@RequestBody GenerateMonsterRequest request, Authentication authentication);

    @GetMapping(value = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    List<MonsterDto> mine(Authentication authentication);
}