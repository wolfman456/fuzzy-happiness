package com.gamer.fowever.tabletopservice.service.map;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-curated map-builder palette for the battle-map editor
 * (design-draft-v2 §6, Slice 3a). Names and swatch colours only — no rulebook
 * text, the {@code ChargenCatalog} precedent. Tile indices are stable: they are
 * persisted as {@code battle_map_tiles.tile_index}, so entries must never be
 * renumbered (append only).
 */
@Component
public class MapCatalog {

    public record TileType(int index, String name, String color) {
    }

    private static final List<TileType> TILES = List.of(
            new TileType(0, "Stone Floor", "#9ca3af"),
            new TileType(1, "Cobblestone", "#78716c"),
            new TileType(2, "Dirt", "#a8a29e"),
            new TileType(3, "Grass", "#4ade80"),
            new TileType(4, "Forest Floor", "#166534"),
            new TileType(5, "Sand", "#fcd34d"),
            new TileType(6, "Wooden Planks", "#b45309"),
            new TileType(7, "Water", "#60a5fa"),
            new TileType(8, "Ice", "#bae6fd"),
            new TileType(9, "Lava", "#f97316"),
            new TileType(10, "Marble", "#e5e7eb"),
            new TileType(11, "Rubble", "#57534e"));

    public List<TileType> tiles() {
        return TILES;
    }

    public boolean hasTile(int index) {
        return TILES.stream().anyMatch(tile -> tile.index() == index);
    }
}
