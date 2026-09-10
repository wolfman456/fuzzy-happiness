package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.CharacterApi;
import com.gamer.fowever.tabletopapi.dto.CharacterDraftDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSheetDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSummaryDto;
import com.gamer.fowever.tabletopapi.dto.CompileResult;
import com.gamer.fowever.tabletopapi.dto.GenerateCharacterRequest;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.service.character.CharacterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CharacterControllerImpl implements CharacterApi {

    private final CharacterService characterService;

    public CharacterControllerImpl(CharacterService characterService) {
        this.characterService = characterService;
    }

    @Override
    public CompileResult compile(@Valid @RequestBody CharacterDraftDto draft, Authentication authentication) {
        return characterService.compile((User) authentication.getPrincipal(), draft);
    }

    @Override
    public CompileResult generate(@Valid @RequestBody GenerateCharacterRequest request, Authentication authentication) {
        return characterService.generate((User) authentication.getPrincipal(), request);
    }

    @Override
    public List<CharacterSummaryDto> mine(Authentication authentication) {
        return characterService.mine((User) authentication.getPrincipal());
    }

    @Override
    public ResponseEntity<CharacterSheetDto> create(@Valid @RequestBody CharacterDraftDto draft,
                                                    Authentication authentication) {
        CharacterSheetDto sheet = characterService.create((User) authentication.getPrincipal(), draft);
        return ResponseEntity.status(HttpStatus.CREATED).body(sheet);
    }

    @Override
    public CharacterSheetDto get(Long id, Authentication authentication) {
        return characterService.get((User) authentication.getPrincipal(), id);
    }
}