package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.CharacterDraftDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSheetDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSummaryDto;
import com.gamer.fowever.tabletopapi.dto.CompileResult;
import com.gamer.fowever.tabletopapi.dto.GenerateCharacterRequest;
import com.gamer.fowever.tabletopapi.dto.RollScoresRequest;
import com.gamer.fowever.tabletopapi.dto.RollScoresResult;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Character generation contract. Endpoints intentionally carry their full paths
 * (two base paths: {@code /api/characters} for the pure compile/generate steps
 * and {@code /api/users/me/characters} for the persisted sheet).
 */
public interface CharacterApi {

    @PostMapping(value = "/api/characters/compile", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CompileResult compile(@Valid @RequestBody CharacterDraftDto draft, Authentication authentication);

    @PostMapping(value = "/api/characters/generate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    CompileResult generate(@Valid @RequestBody GenerateCharacterRequest request, Authentication authentication);

    @PostMapping(value = "/api/characters/roll-scores", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    RollScoresResult rollScores(@Valid @RequestBody RollScoresRequest request, Authentication authentication);

    @GetMapping(value = "/api/users/me/characters", produces = MediaType.APPLICATION_JSON_VALUE)
    List<CharacterSummaryDto> mine(Authentication authentication);

    @PostMapping(value = "/api/users/me/characters", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CharacterSheetDto> create(@Valid @RequestBody CharacterDraftDto draft, Authentication authentication);

    @GetMapping(value = "/api/users/me/characters/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    CharacterSheetDto get(@PathVariable Long id, Authentication authentication);
}