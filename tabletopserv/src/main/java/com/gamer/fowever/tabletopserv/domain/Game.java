package com.gamer.fowever.tabletopserv.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(nullable = false)
    private String displayName;

    @Lob
    @Column(nullable = false)
    private String sheetSchema;

    public Game(String slug, String displayName, String sheetSchema) {
        this.slug = slug;
        this.displayName = displayName;
        this.sheetSchema = sheetSchema;
    }
}