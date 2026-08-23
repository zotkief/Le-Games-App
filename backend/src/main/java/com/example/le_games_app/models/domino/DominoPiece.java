package com.example.le_games_app.models.domino;

import com.example.le_games_app.utils.classes.IntPoint2D;
import jakarta.persistence.*;

@Entity
@Table(name = "domino_pieces")
public class DominoPiece {

    public enum Direction {
    UP,
    DOWN,
    RIGHT,
    LEFT;

    public IntPoint2D getVector() {
        return switch (this) {
            case UP -> new IntPoint2D(0, 1);
            case DOWN -> new IntPoint2D(0, -1);
            case RIGHT -> new IntPoint2D(1, 0);
            case LEFT -> new IntPoint2D(-1, 0);
        };
    }
}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private IntPoint2D position;

    @ManyToOne
    @JoinColumn(name = "domino_game_id", nullable = false)
    private DominoGame game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private String cityName;

    public DominoPiece() {}

    public DominoPiece(DominoGame game, IntPoint2D position, Direction direction, String city) {
        this.game = game;
        this.position = position;
        this.direction = direction;
        this. cityName = city;
    }

    public Long getId() {
        return id;
    }

    public IntPoint2D getPosition() {
        return position;
    }

    public DominoGame getGame() {
        return game;
    }

    public String getDirection() {
        return direction.name();
    }

    public String getCityName() {
        return cityName;
    }

}