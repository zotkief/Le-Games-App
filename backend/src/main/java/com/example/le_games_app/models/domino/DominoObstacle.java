package com.example.le_games_app.models.domino;

import com.example.le_games_app.utils.classes.IntPoint2D;
import jakarta.persistence.*;

@Entity
@Table(name = "domino_obstacles")
public class DominoObstacle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private IntPoint2D position;

    @ManyToOne
    @JoinColumn(name = "domino_game_id", nullable = false)
    private DominoGame game;

    public DominoObstacle() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IntPoint2D getPosition() {
        return position;
    }

    public void setPosition(IntPoint2D position) {
        this.position = position;
    }

    public DominoGame getGame() {
        return game;
    }

    public void setGame(DominoGame game) {
        this.game = game;
    }
}