package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.AddTokenRequest;
import com.gamer.fowever.tabletopserv.dto.BattleMapDto;
import com.gamer.fowever.tabletopserv.dto.CreateMapRequest;
import com.gamer.fowever.tabletopserv.dto.InitiativeRequest;
import com.gamer.fowever.tabletopserv.dto.MoveTokenRequest;
import com.gamer.fowever.tabletopserv.dto.TurnCommandRequest;
import com.gamer.fowever.tabletopserv.dto.UpdateMapRequest;
import com.gamer.fowever.tabletopserv.dto.UpdateTokenRequest;
import com.gamer.fowever.tabletopserv.service.BattleMapService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/map")
public class BattleMapController {

    private final BattleMapService battleMapService;

    public BattleMapController(BattleMapService battleMapService) {
        this.battleMapService = battleMapService;
    }

    @GetMapping
    public BattleMapDto get(@PathVariable Long sessionId, Authentication authentication) {
        return battleMapService.getMap(sessionId);
    }

    @PostMapping
    public ResponseEntity<BattleMapDto> create(@PathVariable Long sessionId,
                                               @Valid @RequestBody CreateMapRequest request,
                                               Authentication authentication) {
        BattleMapDto map = battleMapService.createMap(currentUser(authentication), sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(map);
    }

    @PostMapping("/tokens")
    public ResponseEntity<BattleMapDto> addToken(@PathVariable Long sessionId,
                                                 @Valid @RequestBody AddTokenRequest request,
                                                 Authentication authentication) {
        BattleMapDto map = battleMapService.addToken(currentUser(authentication), sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(map);
    }

    @PatchMapping
    public BattleMapDto update(@PathVariable Long sessionId,
                               @Valid @RequestBody UpdateMapRequest request,
                               Authentication authentication) {
        return battleMapService.updateMap(currentUser(authentication), sessionId, request);
    }

    @PatchMapping("/tokens/{tokenId}")
    public BattleMapDto updateToken(@PathVariable Long sessionId,
                                    @PathVariable Long tokenId,
                                    @Valid @RequestBody UpdateTokenRequest request,
                                    Authentication authentication) {
        return battleMapService.updateToken(currentUser(authentication), sessionId, tokenId, request);
    }

    @DeleteMapping("/tokens/{tokenId}")
    public ResponseEntity<Void> removeToken(@PathVariable Long sessionId,
                                            @PathVariable Long tokenId,
                                            Authentication authentication) {
        battleMapService.removeToken(currentUser(authentication), sessionId, tokenId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tokens/{tokenId}/move")
    public BattleMapDto moveToken(@PathVariable Long sessionId,
                                  @PathVariable Long tokenId,
                                  @Valid @RequestBody MoveTokenRequest request,
                                  Authentication authentication) {
        return battleMapService.moveToken(currentUser(authentication), sessionId, tokenId, request);
    }

    @PostMapping("/turn")
    public BattleMapDto turn(@PathVariable Long sessionId,
                             @Valid @RequestBody TurnCommandRequest request,
                             Authentication authentication) {
        return battleMapService.turnCommand(currentUser(authentication), sessionId, request);
    }

    @PostMapping("/initiative")
    public BattleMapDto setInitiative(@PathVariable Long sessionId,
                                      @Valid @RequestBody InitiativeRequest request,
                                      Authentication authentication) {
        return battleMapService.setInitiative(currentUser(authentication), sessionId, request);
    }

    @PostMapping("/initiative/{entryId}/reroll")
    public BattleMapDto rerollInitiative(@PathVariable Long sessionId,
                                         @PathVariable Long entryId,
                                         Authentication authentication) {
        return battleMapService.rerollInitiative(currentUser(authentication), sessionId, entryId);
    }

    @PostMapping("/initiative/next")
    public BattleMapDto nextInitiative(@PathVariable Long sessionId,
                                       Authentication authentication) {
        return battleMapService.nextInitiative(currentUser(authentication), sessionId);
    }

    @DeleteMapping("/initiative/{entryId}")
    public BattleMapDto removeInitiativeEntry(@PathVariable Long sessionId,
                                              @PathVariable Long entryId,
                                              Authentication authentication) {
        return battleMapService.removeInitiativeEntry(currentUser(authentication), sessionId, entryId);
    }

    private User currentUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}