package com.gamer.fowever.tabletopapi.dto;

import java.util.List;

/**
 * The hand-curated map-builder catalog: the tile palette the GM paints with
 * (design-draft-v2 §6, Slice 3a). Exposed at {@code GET /api/sessions/{id}/map/catalog}.
 * Object defs join this catalog in Slice 3b.
 */
public record MapCatalogDto(List<MapTileTypeDto> tiles) {
}
