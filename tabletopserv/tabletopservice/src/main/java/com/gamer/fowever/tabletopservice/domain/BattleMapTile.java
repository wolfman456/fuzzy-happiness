package com.gamer.fowever.tabletopservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A painted terrain swatch on one battle-map square (design-draft-v2 §6, Slice 3a).
 * One row per non-empty cell; erasing a square deletes the row. The
 * {@code (map_id, pos_x, pos_y)} unique constraint keeps the grid single-valued.
 */
@Entity
@Table(name = "battle_map_tiles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"map_id", "pos_x", "pos_y"}))
@Getter
@Setter
@NoArgsConstructor
public class BattleMapTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false, updatable = false)
    private BattleMap map;

    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(name = "tile_index", nullable = false)
    private int tileIndex;

    public BattleMapTile(BattleMap map, int posX, int posY, int tileIndex) {
        this.map = map;
        this.posX = posX;
        this.posY = posY;
        this.tileIndex = tileIndex;
    }
}
