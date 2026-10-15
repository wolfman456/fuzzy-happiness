package com.gamer.fowever.tabletopapi.dto;

import java.util.List;

/**
 * The hand-curated map-builder catalog: the tile palette the GM paints with and
 * the object defs the GM places (design-draft-v2 §6, Slices 3a/3b). Exposed at
 * {@code GET /api/sessions/{id}/map/catalog}.
 */
public record MapCatalogDto(List<MapTileTypeDto> tiles, List<MapObjectTypeDto> objects) {
}
