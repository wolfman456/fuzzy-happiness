package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.Game;

public record GameSummary(String slug, String displayName, String sheetSchema) {

    public static GameSummary from(Game game) {
        return new GameSummary(game.getSlug(), game.getDisplayName(), game.getSheetSchema());
    }
}