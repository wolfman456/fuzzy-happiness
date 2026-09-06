package com.gamer.fowever.tabletopserv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "map_tokens")
@Getter
@Setter
@NoArgsConstructor
public class MapToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false, updatable = false)
    private BattleMap map;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenCategory category;

    @Column(nullable = false)
    private String color;

    @Column(name = "speed_feet", nullable = false)
    private int speedFeet;

    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(name = "moved_feet", nullable = false)
    private int movedFeet;

    @Column(name = "linked_participant_id")
    private Long linkedParticipantId;

    @Column(name = "linked_user_id")
    private Long linkedUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MapToken(BattleMap map, String name, TokenCategory category, String color, int speedFeet,
                    int posX, int posY, Long linkedParticipantId, Long linkedUserId) {
        this.map = map;
        this.name = name;
        this.category = category;
        this.color = color;
        this.speedFeet = speedFeet;
        this.posX = posX;
        this.posY = posY;
        this.movedFeet = 0;
        this.linkedParticipantId = linkedParticipantId;
        this.linkedUserId = linkedUserId;
    }
}