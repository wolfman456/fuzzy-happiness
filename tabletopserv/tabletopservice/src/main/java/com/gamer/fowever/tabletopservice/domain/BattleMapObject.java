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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A placed environment item on a battle map (design-draft-v2 §6, Slice 3b). The
 * footprint is an axis-aligned {@code width}×{@code height} block of squares
 * anchored at {@code (posX, posY)}; {@code blocksMovement} marks those squares
 * impassable. {@code rotation} is a visual quarter-turn only — it does not
 * change the blocking footprint (MVP, noted as a follow-up).
 */
@Entity
@Table(name = "battle_map_objects")
@Getter
@Setter
@NoArgsConstructor
public class BattleMapObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false, updatable = false)
    private BattleMap map;

    @Column(name = "object_index", nullable = false)
    private int objectIndex;

    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(name = "width", nullable = false)
    private int width;

    @Column(name = "height", nullable = false)
    private int height;

    @Column(name = "rotation", nullable = false)
    private int rotation;

    @Column(name = "blocks_movement", nullable = false)
    private boolean blocksMovement;

    public BattleMapObject(BattleMap map, int objectIndex, int posX, int posY,
                           int width, int height, int rotation, boolean blocksMovement) {
        this.map = map;
        this.objectIndex = objectIndex;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.blocksMovement = blocksMovement;
    }
}
