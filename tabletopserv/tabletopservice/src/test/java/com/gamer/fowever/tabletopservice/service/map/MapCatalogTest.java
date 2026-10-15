package com.gamer.fowever.tabletopservice.service.map;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapCatalogTest {

    private final MapCatalog catalog = new MapCatalog();

    @Test
    void paletteIsNonEmptyWithStableZeroBasedIndices() {
        assertThat(catalog.tiles()).isNotEmpty();
        for (int i = 0; i < catalog.tiles().size(); i += 1) {
            assertThat(catalog.tiles().get(i).index()).isEqualTo(i);
        }
    }

    @Test
    void paletteEntriesCarryNameAndColor() {
        for (MapCatalog.TileType tile : catalog.tiles()) {
            assertThat(tile.name()).isNotBlank();
            assertThat(tile.color()).matches("#[0-9a-fA-F]{6}");
        }
    }

    @Test
    void paletteIndicesAreUnique() {
        Set<Integer> indices = new HashSet<>();
        for (MapCatalog.TileType tile : catalog.tiles()) {
            assertThat(indices.add(tile.index())).isTrue();
        }
    }

    @Test
    void hasTileRecognizesKnownAndUnknownIndices() {
        assertThat(catalog.hasTile(0)).isTrue();
        assertThat(catalog.hasTile(catalog.tiles().size() - 1)).isTrue();
        assertThat(catalog.hasTile(-1)).isFalse();
        assertThat(catalog.hasTile(catalog.tiles().size())).isFalse();
    }
}
