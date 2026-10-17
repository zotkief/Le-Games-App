package com.example.le_games_app.models.domino;

import jakarta.persistence.*;

import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.*;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.utils.classes.IntPoint2D;

@Entity
@Table(name = "domino_games")
public class DominoGame {

    private static int R=7;
    private static double OBSTACLE_CHANCE=0.15;

    private Random random;

    public enum GameState {
        ONGOING,
        FINISHED
    }

    public enum GameType {
        DAILY,
        PRACTICE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameState gameState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Column()
    private LocalDate date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_identity_id", nullable = false)
    private PlayerIdentity identity;

    @OneToMany(
        mappedBy = "game",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<DominoObstacle> obstacles = new ArrayList<>();


    @OneToMany(
        mappedBy = "game",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<DominoPiece> pieces = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "begin_x")),
        @AttributeOverride(name = "y", column = @Column(name = "begin_y"))
    })
    private IntPoint2D beginPos;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "end_x")),
        @AttributeOverride(name = "y", column = @Column(name = "end_y"))
    })
    private IntPoint2D endPos;

    @Column(nullable = false)
    private Character endLetter;

    @Column(nullable = false)
    private Character beginLetter;


    @Column(nullable = false)
    private boolean isComplete = false;

    public void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public DominoGame() throws RuntimeException, IllegalArgumentException {
        random = new Random();
        this.gameState = GameState.ONGOING;
        this.gameType = GameType.PRACTICE;
        this.date = null;
        generateMap();
        try{
            putFirstPiece();
        }
        catch(Exception e)
        {
            throw new RuntimeException();
        }
    }

    public DominoGame(LocalDate date) throws RuntimeException, IllegalArgumentException {
        long seed = date.getYear() * 12 * 35 + 
                    date.getMonth().ordinal() * 35 + 
                    date.getDayOfMonth();
        random = new Random(seed);
        this.gameState = GameState.ONGOING;
        this.gameType = GameType.DAILY;
        this.date = date;
        generateMap();
        try{
            putFirstPiece();
        }
        catch(Exception e)
        {
            throw new RuntimeException();
        }
    }

    public boolean putPiece(IntPoint2D pos,
                            DominoPiece.Direction direction,
                            String city) {

        if (gameState == GameState.FINISHED) {
            return false;
        }

        city = city.toUpperCase();

        IntPoint2D pieceEnd = pos.copy();
        pieceEnd.addPoint(direction.getVector());

        IntPoint2D[] pieceEnds = {pos, pieceEnd};
        Character[] pieceLetters = {
                city.charAt(0),
                city.charAt(city.length() - 1)
        };

        IntPoint2D[] dominoEnds = {beginPos, endPos};
        Character[] dominoLetters = {beginLetter, endLetter};

        int dominoIdx = -1;
        int pieceIdx = -1;

        // znajdź poprawne połączenie
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                System.out.println(dominoEnds[i].toString());
                System.out.println(pieceEnds[j].toString());
                
                System.out.println(dominoLetters[i]);
                System.out.println(pieceLetters[j]);
                if (
                        dominoEnds[i].squareDist(pieceEnds[j]) == 1 &&
                        dominoLetters[i].equals(pieceLetters[j])
                ) {
                    dominoIdx = i;
                    pieceIdx = j;
                    break;
                }
            }

            if (dominoIdx != -1)
                break;
        }

        if (dominoIdx == -1) {
            System.out.println("idx -1");
            return false;
        }

        // sprawdzenie przeszkód
        for (DominoObstacle obstacle : obstacles) {
            if (
                    obstacle.getPosition().squareDist(pieceEnds[0]) == 0 ||
                    obstacle.getPosition().squareDist(pieceEnds[1]) == 0
            ) {
                System.out.println("obstacle");
                return false;
            }
        }

        // sprawdzenie istniejących klocków
        for (DominoPiece piece : pieces) {

            IntPoint2D oldBegin = piece.getPosition();

            IntPoint2D oldEnd = oldBegin.copy();
            oldEnd.addPoint(
                    DominoPiece.Direction
                            .valueOf(piece.getDirection())
                            .getVector()
            );

            IntPoint2D[] oldEnds = {oldBegin, oldEnd};

            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {

                    if (pieceEnds[a].squareDist(oldEnds[b]) == 0) {
                        return false;
                    }
                }
            }
        }

        // dodanie klocka
        pieces.add(new DominoPiece(
                this,
                pos,
                direction,
                city
        ));

        // który koniec nowego klocka pozostaje wolny
        int freePieceEnd = (pieceIdx + 1) % 2;

        if (dominoIdx == 0) {
            beginPos = pieceEnds[freePieceEnd];
            beginLetter = pieceLetters[freePieceEnd];
        } else {
            endPos = pieceEnds[freePieceEnd];
            endLetter = pieceLetters[freePieceEnd];
        }

        // A loop is closed when its two free ends become adjacent.
        if (beginPos.squareDist(endPos) == 1) {
            gameState = GameState.FINISHED;
        }

        return true;
    }

    private void putFirstPiece() throws Exception{
        try (RandomAccessFile raf = new RandomAccessFile("data/cities.csv", "r")) {

            long left = 0;
            long right = raf.length();

            long target = left + (long)(random.nextDouble() * (right - left));

            raf.seek(target);

            if (target != 0) {
                raf.seek(target - 1);

                int c;
                while (raf.getFilePointer() > 0) {
                    c = raf.read();
                    if (c == '\n') {
                        break;
                    }
                    raf.seek(raf.getFilePointer() - 2);
                }
            }

            String line = raf.readLine();

            String city = line.split(";")[2].toUpperCase();

            DominoPiece.Direction dir;
            switch (random.nextInt(1,5)) {
                case 1:
                    dir = DominoPiece.Direction.DOWN;
                    break;
                case 2:
                    dir = DominoPiece.Direction.UP;
                    break;
                case 3:
                    dir = DominoPiece.Direction.RIGHT;
                    break;
                default:
                    dir = DominoPiece.Direction.LEFT;
                    break;
            }
            pieces.add(new DominoPiece(this,new IntPoint2D(0, 0),dir,city.toUpperCase()));

            this.beginLetter = city.charAt(0);
            this.endLetter = city.charAt(city.length()-1);

            this.beginPos = new IntPoint2D(0,0);
            this.endPos = new IntPoint2D(0,0);
            this.endPos.addPoint(dir.getVector());
        }
        catch(Exception e){
            throw new Exception();
        }
    }

    public Long getId() {
        return id;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public PlayerIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(PlayerIdentity identity) {
        this.identity = identity;
    }

    public List<DominoObstacle> getObstacles() {
        return this.obstacles;
    }

    private boolean inCircle(int x,int y,int r) {
        return x * x + y * y <= r * r;
    }

    private boolean inSafeZone(int x,int y) {
        return x*x+y*y < 8;
    }

    private void generateMap() {
        for (int i = -R; i <= R; i++) {
            for (int j = -R; j <= R; j++) {
                if(!inSafeZone(i, j) 
                    && inCircle(j, i, DominoGame.R) 
                    && random.nextDouble() < DominoGame.OBSTACLE_CHANCE)
                {
                    addObstacle(new IntPoint2D(j, i));
                }
            }
        }
    }

    private void addObstacle(IntPoint2D position) {
        DominoObstacle obstacle = new DominoObstacle();

        obstacle.setPosition(position);
        obstacle.setGame(this);

        obstacles.add(obstacle);
    }

    public List<DominoPiece> getPieces() {
        return pieces;
    }
}