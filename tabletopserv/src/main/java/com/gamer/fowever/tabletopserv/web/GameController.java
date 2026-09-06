package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.dto.GameSummary;
import com.gamer.fowever.tabletopserv.repository.GameRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;

    public GameController(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public List<GameSummary> listGames() {
        return gameRepository.findAll().stream().map(GameSummary::from).toList();
    }
}