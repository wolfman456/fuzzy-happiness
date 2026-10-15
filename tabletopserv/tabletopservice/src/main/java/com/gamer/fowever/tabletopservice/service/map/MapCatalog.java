package com.gamer.fowever.tabletopservice.service.map;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Hand-curated map-builder catalog for the battle-map editor
 * (design-draft-v2 §6, Slices 3a/3b). Names, swatch colours and default
 * footprints only — no rulebook text, the {@code ChargenCatalog} precedent.
 * Indices are stable: tile indices are persisted as
 * {@code battle_map_tiles.tile_index} and object indices as
 * {@code battle_map_objects.object_index}, so entries must never be renumbered
 * (append only).
 */
@Component
public class MapCatalog {

    public record TileType(int index, String name, String color) {
    }

    /**
     * A placeable environment item. {@code width}/{@code height} are the default
     * footprint in squares; {@code blocksMovement} is the default for whether the
     * footprint is impassable (per-instance overridable when placed).
     */
    public record ObjectType(int index, String name, String color, int width, int height, boolean blocksMovement) {
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

    private static final List<ObjectType> OBJECTS = List.of(
            new ObjectType(0, "Wall", "#6b7280", 1, 1, true),
            new ObjectType(1, "Stone Pillar", "#57534e", 1, 1, true),
            new ObjectType(2, "Tree", "#15803d", 1, 1, true),
            new ObjectType(3, "Boulder", "#78716c", 1, 1, true),
            new ObjectType(4, "Table", "#a16207", 2, 2, true),
            new ObjectType(5, "Barrel", "#92400e", 1, 1, true),
            new ObjectType(6, "Rug", "#b91c1c", 2, 2, false),
            new ObjectType(7, "Campfire", "#ea580c", 1, 1, false),
            new ObjectType(8, "Bookshelf", "#78350f", 3, 1, true),
            new ObjectType(9, "Debris", "#57534e", 1, 1, false));

    public List<TileType> tiles() {
        return TILES;
    }

    public boolean hasTile(int index) {
        return TILES.stream().anyMatch(tile -> tile.index() == index);
    }

    public List<ObjectType> objects() {
        return OBJECTS;
    }

    public boolean hasObject(int index) {
        return OBJECTS.stream().anyMatch(object -> object.index() == index);
    }

    public Optional<ObjectType> object(int index) {
        return OBJECTS.stream().filter(object -> object.index() == index).findFirst();
    }
}
