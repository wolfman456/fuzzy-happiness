package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.AddTokenRequest;
import com.gamer.fowever.tabletopapi.dto.BattleMapDto;
import com.gamer.fowever.tabletopapi.dto.CreateMapRequest;
import com.gamer.fowever.tabletopapi.dto.InitiativeRequest;
import com.gamer.fowever.tabletopapi.dto.MoveTokenRequest;
import com.gamer.fowever.tabletopapi.dto.TurnCommandRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateMapRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/sessions/{sessionId}/map")
public interface BattleMapApi {

    @GetMapping
    BattleMapDto get(@PathVariable Long sessionId, Authentication authentication);

    @PostMapping
    ResponseEntity<BattleMapDto> create(@PathVariable Long sessionId,
                                        @Valid @RequestBody CreateMapRequest request,
                                        Authentication authentication);

    @PostMapping("/tokens")
    ResponseEntity<BattleMapDto> addToken(@PathVariable Long sessionId,
                                          @Valid @RequestBody AddTokenRequest request,
                                          Authentication authentication);

    @PatchMapping
    BattleMapDto update(@PathVariable Long sessionId,
                        @Valid @RequestBody UpdateMapRequest request,
                        Authentication authentication);

    @PatchMapping("/tokens/{tokenId}")
    BattleMapDto updateToken(@PathVariable Long sessionId,
                             @PathVariable Long tokenId,
                             @Valid @RequestBody UpdateTokenRequest request,
                             Authentication authentication);

    @DeleteMapping("/tokens/{tokenId}")
    ResponseEntity<Void> removeToken(@PathVariable Long sessionId,
                                     @PathVariable Long tokenId,
                                     Authentication authentication);

    @PostMapping("/tokens/{tokenId}/move")
    BattleMapDto moveToken(@PathVariable Long sessionId,
                           @PathVariable Long tokenId,
                           @Valid @RequestBody MoveTokenRequest request,
                           Authentication authentication);

    @PostMapping("/turn")
    BattleMapDto turn(@PathVariable Long sessionId,
                      @Valid @RequestBody TurnCommandRequest request,
                      Authentication authentication);

    @PostMapping("/initiative")
    BattleMapDto setInitiative(@PathVariable Long sessionId,
                               @Valid @RequestBody InitiativeRequest request,
                               Authentication authentication);

    @PostMapping("/initiative/{entryId}/reroll")
    BattleMapDto rerollInitiative(@PathVariable Long sessionId,
                                  @PathVariable Long entryId,
                                  Authentication authentication);

    @PostMapping("/initiative/next")
    BattleMapDto nextInitiative(@PathVariable Long sessionId,
                                Authentication authentication);

    @DeleteMapping("/initiative/{entryId}")
    BattleMapDto removeInitiativeEntry(@PathVariable Long sessionId,
                                       @PathVariable Long entryId,
                                       Authentication authentication);
}