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

@Entity
@Table(name = "battle_maps", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id"}))
@Getter
@Setter
@NoArgsConstructor
public class BattleMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    private GameSession session;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "square_feet", nullable = false)
    private int squareFeet;

    @Column(name = "current_turn_token_id")
    private Long currentTurnTokenId;

    @Column(name = "initiative_index", nullable = false)
    private int initiativeIndex = -1;

    public BattleMap(GameSession session, String name, int width, int height, int squareFeet) {
        this.session = session;
        this.name = name;
        this.width = width;
        this.height = height;
        this.squareFeet = squareFeet;
    }
}