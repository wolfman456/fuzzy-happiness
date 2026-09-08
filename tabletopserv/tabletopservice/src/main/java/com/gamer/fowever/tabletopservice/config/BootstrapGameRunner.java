package com.gamer.fowever.tabletopservice.config;

import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class BootstrapGameRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapGameRunner.class);

    private final GameRepository gameRepository;

    public BootstrapGameRunner(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (gameRepository.existsBySlug("dnd-5e")) {
            return;
        }
        Game game = new Game("dnd-5e", "D&D 5e",
                """
                {"groups":[{"fields":[{"key":"race","type":"text"},{"key":"class","type":"text"}]}]}
                """);
        gameRepository.save(game);
        log.info("Bootstrap game registered: {}", game.getSlug());
    }
}