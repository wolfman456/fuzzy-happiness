package com.gamer.fowever.tabletopserv.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "character_drafts")
@Getter
@Setter
@NoArgsConstructor
public class CharacterDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private int strength;

    @Column(nullable = false)
    private int dexterity;

    @Column(nullable = false)
    private int constitution;

    @Column(nullable = false)
    private int intelligence;

    @Column(nullable = false)
    private int wisdom;

    @Column(nullable = false)
    private int charisma;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_source", nullable = false)
    private ScoreSource scoreSource;

    @Column(name = "starting_level", nullable = false)
    private int startingLevel;

    @Column(name = "race_index")
    private String raceIndex;

    @Column(name = "class_index")
    private String classIndex;

    @Column(name = "subclass_index")
    private String subclassIndex;

    @Column(name = "background_index")
    private String backgroundIndex;

    @ElementCollection
    @CollectionTable(name = "draft_skill_picks", joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "skill_index")
    private Set<String> skillPickIndexes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "draft_spells", joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "spell_index")
    private Set<String> spellIndexes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "draft_equipment", joinColumns = @JoinColumn(name = "draft_id"))
    @Column(name = "equipment_index")
    private Set<String> equipmentIndexes = new HashSet<>();
}