package com.gamer.fowever.tabletopapi.dto;

import java.util.List;

/**
 * The hand-curated chargen catalog (2014 PHB beyond the SRD feed): the full PHB
 * background list and each class's PHB subclass options with their starting
 * level. Exposed at {@code GET /api/characters/catalog} so the wizard can offer
 * PHB choices the SRD feed does not cover.
 */
public record ChargenCatalogDto(List<BackgroundOption> backgrounds, List<SubclassOption> subclasses) {

    public record BackgroundOption(String index, String name) {
    }

    public record SubclassOption(String classIndex, String index, String name, int level) {
    }
}