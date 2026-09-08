package com.gamer.fowever.tabletopservice.web;

import com.gamer.fowever.tabletopapi.GameApi;
import com.gamer.fowever.tabletopapi.dto.GameSummary;
import com.gamer.fowever.tabletopservice.repository.GameRepository;
import com.gamer.fowever.tabletopservice.support.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameControllerImpl implements GameApi {

    private final GameRepository gameRepository;

    public GameControllerImpl(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @GetMapping
    @Override
    public List<GameSummary> listGames() {
        return gameRepository.findAll().stream().map(Dtos::gameSummary).toList();
    }
}