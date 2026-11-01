package com.example.le_games_app.models;

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

@Entity
@Table(
        name = "guess_city_rounds",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guess_city_round_puzzle_player",
                columnNames = {"guess_city_game_id", "player_identity_id"}
        )
)
public class GuessCityRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guess_city_game_id", nullable = false)
    private GuessCityGame puzzle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_identity_id", nullable = false)
    private PlayerIdentity player;

    @Column(name = "in_progress", nullable = false)
    private boolean inProgress;

    protected GuessCityRound() {
    }

    public GuessCityRound(GuessCityGame puzzle, PlayerIdentity player) {
        this.puzzle = puzzle;
        this.player = player;
        this.inProgress = true;
    }

    public Long getId() {
        return id;
    }

    public GuessCityGame getPuzzle() {
        return puzzle;
    }

    public PlayerIdentity getPlayer() {
        return player;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void finish() {
        inProgress = false;
    }
}
