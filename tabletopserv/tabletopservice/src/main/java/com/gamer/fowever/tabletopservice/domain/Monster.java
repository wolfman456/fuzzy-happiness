package com.gamer.fowever.tabletopservice.domain;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "monsters")
@Getter
@Setter
@NoArgsConstructor
public class Monster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "cr", nullable = false)
    private String cr;

    @Enumerated(EnumType.STRING)
    @Column(name = "combat_role", nullable = false)
    private MonsterRole combatRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MonsterEdition edition;

    @Lob
    @Column(name = "statblock_json", nullable = false)
    private String statblockJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Monster(User owner, String name, String cr, MonsterRole combatRole,
                   MonsterEdition edition, String statblockJson, Instant createdAt) {
        this.owner = owner;
        this.name = name;
        this.cr = cr;
        this.combatRole = combatRole;
        this.edition = edition;
        this.statblockJson = statblockJson;
        this.createdAt = createdAt;
    }
}