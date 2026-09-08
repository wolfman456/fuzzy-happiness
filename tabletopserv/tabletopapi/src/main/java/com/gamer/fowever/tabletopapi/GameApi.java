package com.gamer.fowever.tabletopapi;

import com.gamer.fowever.tabletopapi.dto.GameSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/api/games")
public interface GameApi {

    @GetMapping
    List<GameSummary> listGames();
}