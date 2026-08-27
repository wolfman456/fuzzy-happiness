package com.gamer.fowever.tabletopserv.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("DND5E")
@Getter
@Setter
public class Dnd5eCharacter extends Character {

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

    @Column(nullable = false)
    private int level;

    @Column(name = "score_source")
    private ScoreSource scoreSource;

    @Column(name = "race_index")
    private String raceIndex;

    @Column(name = "class_index")
    private String classIndex;

    @Column(name = "subclass_index")
    private String subclassIndex;

    @Column(name = "background_index")
    private String backgroundIndex;

    @ElementCollection
    @CollectionTable(name = "character_skill_picks", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "skill_index")
    private Set<String> skillPickIndexes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "character_spells", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "spell_index")
    private Set<String> spellIndexes = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "character_features", joinColumns = @JoinColumn(name = "character_id"))
    @Column(name = "feature_index")
    private Set<String> featureIndexes = new HashSet<>();

    private int hitPoints;

    private int armorClass;

    private int proficiencyBonus;

    @Lob
    @Column(name = "sheet_snapshot")
    private String sheetSnapshot;
}