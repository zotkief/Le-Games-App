package com.example.le_games_app.controller;

import com.example.le_games_app.models.domino.DominoGame;
import com.example.le_games_app.models.domino.DominoPiece;
import com.example.le_games_app.models.domino.DominoObstacle;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.security.JwtService;
import com.example.le_games_app.support.components.CityIndex;
import com.example.le_games_app.utils.classes.IntPoint2D;
import com.example.le_games_app.repository.DominoGameRepository;

import java.util.ArrayList;
import java.util.UUID;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import com.example.le_games_app.models.PlayerIdentity;

import java.util.List;

@RestController
@RequestMapping("/api/domino")
public class DominoController {

    private final CityIndex cityIndex;
    private final PlayerIdentityRepository identityRepository;
    private final DominoGameRepository dominoGameRepository;
    private final JwtService jwtService;

    public DominoController(PlayerIdentityRepository identityRepository,
                          DominoGameRepository dominoGameRepository,
                          JwtService jwtService,
                          CityIndex cityIndex) {
        this.cityIndex = cityIndex;
        this.identityRepository = identityRepository;
        this.dominoGameRepository = dominoGameRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/move")
    public ResponseEntity<GameResponse> move(@RequestBody MoveRequest request) {

        String query = request.getCityName().toUpperCase();


        if (!cityIndex.find(query)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "city not found"
            );
        }


        String city = query;

        IntPoint2D newPoint = new IntPoint2D(request.getX(), request.getY());
        DominoPiece.Direction direction = request.getDirection();
        long gameId = request.getGameId();

        DominoGame game = dominoGameRepository.getReferenceById(gameId);
        DominoPiece p = game.getPieces().get(0);
        System.out.println(p.getDirection().toString());
        System.out.println(p.getPosition().getX());
        System.out.println(p.getPosition().getY());
        System.out.println(p.getCityName());

        if (!game.putPiece(newPoint, direction, city)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "put wrong piece"
            );
        }


        boolean isComplete = game.getGameState().equals(DominoGame.GameState.FINISHED);

        game.setComplete(isComplete);

        dominoGameRepository.save(game);

        GameResponse response = new GameResponse(game);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<SuggestionResponse> suggestions(
            @RequestParam String text
    ) {
        List<String> result = cityIndex.suggest(text, 10);
        return ResponseEntity.ok(new SuggestionResponse(result));
    }

    @GetMapping("/games")
    public ResponseEntity<UserGamesResponse> getUserGames(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }
        
        String token = authHeader.substring(7);
        Claims claims = jwtService.parseToken(token);
        UUID playerUid = UUID.fromString(claims.get("uid").toString());
        
        PlayerIdentity identity = identityRepository.findById(playerUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player identity not found"
                ));
        
        List<DominoGame> games = dominoGameRepository.findOngoingGamesByIdentity(identity);
        List<Long> gameIds = new ArrayList<>();
        for (DominoGame game : games) {
            gameIds.add(game.getId());
        }
        
        return ResponseEntity.ok(new UserGamesResponse(gameIds));
    }

    @GetMapping("/game/{gameId}")
    public ResponseEntity<GameResponse> getGameState(@PathVariable Long gameId) {
        
        DominoGame game = dominoGameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Game not found"
                ));
        
        GameResponse response = new GameResponse(game);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/newgame")
    public ResponseEntity<GameResponse> newGame(
            @RequestBody NewGameRequest request,
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header"
            );
        }
        
        String token = authHeader.substring(7);
        Claims claims = jwtService.parseToken(token);
        UUID playerUid = UUID.fromString(claims.get("uid").toString());
        
        PlayerIdentity identity = identityRepository.findById(playerUid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player identity not found"
                ));
        
        LocalDate date = request.getDate();
        DominoGame game;
        
        if (date != null) {
            if (dominoGameRepository.findDailyGameByIdentityAndDate(identity, date).isPresent()) {
                game = dominoGameRepository.findDailyGameByIdentityAndDate(identity, date).get();
            }
            else {
                game = new DominoGame(date);
            }
        } else {
            game = new DominoGame();
        }

        game.setIdentity(identity);
        game = dominoGameRepository.save(game);

        GameResponse response = new GameResponse(game);

        return ResponseEntity.ok(response);
    }

    public static class UserGamesResponse {
        private List<Long> gameIds;

        public UserGamesResponse(List<Long> gameIds) {
            this.gameIds = gameIds;
        }

        public UserGamesResponse() {}

        public List<Long> getGameIds() {
            return gameIds;
        }

        public void setGameIds(List<Long> gameIds) {
            this.gameIds = gameIds;
        }
    }

    public static class NewGameRequest {
        private LocalDate date;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }
    }

    public static class MoveRequest {
        private int x,y;
        private String cityName;
        private DominoPiece.Direction direction;
        private int gameId;

        public int getGameId() {
            return gameId;
        }
        public void setGameId(int gameId) {
            this.gameId = gameId;
        }
        public int getX() {
            return x;
        }
        public void setX(int x) {
            this.x = x;
        }
        public int getY() {
            return y;
        }
        public void setY(int y) {
            this.y = y;
        }
        public String getCityName() {
            return cityName;
        }
        public void setCityName(String cityName) {
            this.cityName = cityName;
        }
        public DominoPiece.Direction getDirection() {
            return direction;
        }
        public void setDirection(String direction) {
            this.direction = DominoPiece.Direction.valueOf(direction.toUpperCase());
        }
    }

    public static class SuggestionRequest {
        private String text;

        public SuggestionRequest(String text) {
            this.text = text;
        }

        public SuggestionRequest() {}

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
    
    public static class SuggestionResponse {
        private List<String> suggestions;

        public SuggestionResponse(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public SuggestionResponse() {}

        public List<String> getSuggestions() {
            return suggestions;
        }
    }

    public static class GameResponse {
        private final List<IntPoint2D> obstacles;
        private final List<DominoPieceItem> pieces;
        private final boolean isComplete;
        private final long gameId;

        public GameResponse(DominoGame game) {
            this.obstacles = new ArrayList<IntPoint2D>();
            for (DominoObstacle obstacle : game.getObstacles()) {
                this.obstacles.add(obstacle.getPosition());
            }
            this.gameId = game.getId();

            this.pieces = new ArrayList<DominoPieceItem>();
            for (DominoPiece piece : game.getPieces()) {
                this.pieces.add(new DominoPieceItem(
                    piece.getPosition(), 
                    piece.getDirection(), 
                    piece.getCityName())
                );
            }

            this.isComplete = game.isComplete();
        }

        public List<IntPoint2D> getObstacles() {
            return obstacles;
        }

        public List<DominoPieceItem> getPieces() {
            return pieces;
        }

        public boolean isComplete() {
            return this.isComplete;
        }

        public Long getGameId()
        {
            return this.gameId;
        }
    }

    public static class DominoPieceItem {
        private IntPoint2D pos;
        private String direction;
        private String cityName;
        public DominoPieceItem(IntPoint2D pos, String direction, String cityName) {
            this.pos = pos;
            this.direction = direction;
            this.cityName = cityName;
        }
        public IntPoint2D getPos() {
            return pos;
        }
        public void setPos(IntPoint2D pos) {
            this.pos = pos;
        }
        public String getDirection() {
            return direction;
        }
        public void setDirection(String direction) {
            this.direction = direction;
        }
        public String getCityName() {
            return cityName;
        }
        public void setCityName(String cityName) {
            this.cityName = cityName;
        }
    }


}
