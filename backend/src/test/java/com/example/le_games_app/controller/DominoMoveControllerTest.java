package com.example.le_games_app.controller;

import com.example.le_games_app.controller.DominoController.SuggestionResponse;
// SuggestionRequest no longer needed; suggestions sent via query param
import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.models.domino.DominoGame;
import com.example.le_games_app.models.domino.DominoPiece;
import com.example.le_games_app.utils.classes.IntPoint2D;
import com.example.le_games_app.repository.DominoGameRepository;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.repository.UserRepository;
import com.example.le_games_app.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "springdoc.api-docs.enabled=true",
    "springdoc.swagger-ui.enabled=true"
})
class DominoMoveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerIdentityRepository identityRepository;

    @Autowired
    private DominoGameRepository dominoGameRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        dominoGameRepository.deleteAll();
        identityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void moveSucceeds_addsPiece() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);
        String token = jwtService.generateToken(identity, null);

        DominoGame game = new DominoGame();
        game.setIdentity(identity);
        DominoGame saved = dominoGameRepository.save(game);

        DominoPiece first = saved.getPieces().get(0);

        int px = first.getPosition().getX();
        int py = first.getPosition().getY();

        DominoPiece.Direction dir = DominoPiece.Direction.valueOf(first.getDirection());
        int ex = px + dir.getVector().getX();
        int ey = py + dir.getVector().getY();

        // place new piece two steps away and point back to connect
        int nx = ex + dir.getVector().getX();
        int ny = ey + dir.getVector().getY();

        DominoPiece.Direction opp;
        if (dir == DominoPiece.Direction.UP) opp = DominoPiece.Direction.RIGHT;
        else if (dir == DominoPiece.Direction.DOWN) opp = DominoPiece.Direction.RIGHT;
        else if (dir == DominoPiece.Direction.LEFT) opp = DominoPiece.Direction.UP;
        else opp = DominoPiece.Direction.UP;

        char lastLetter = first.getCityName().charAt(first.getCityName().length() - 1);

        String suggestionsJson = mockMvc.perform(
                get("/api/domino/suggestions")
                    .param("text", String.valueOf(lastLetter))
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        SuggestionResponse suggestions =
            objectMapper.readValue(suggestionsJson, SuggestionResponse.class);

        String nextCity = suggestions.getSuggestions().get(0);

        Map<String,Object> payload = Map.of(
            "x", nx,
            "y", ny,
            "direction", opp.name(),
            "gameId", saved.getId().intValue(),
            "cityName", nextCity
        );

        int prevSize = saved.getPieces().size();

        mockMvc.perform(post("/api/domino/move")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.obstacles").isArray())
        .andExpect(jsonPath("$.pieces").isArray())
        .andExpect(jsonPath("$.complete").exists());

        DominoGame after = dominoGameRepository.findById(saved.getId()).get();
        assertEquals(prevSize + 1, after.getPieces().size());
    }

    @Test
    void moveFails_badRequest() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);
        String token = jwtService.generateToken(identity, null);

        DominoGame game = new DominoGame();
        game.setIdentity(identity);
        DominoGame saved = dominoGameRepository.save(game);

        Map<String,Object> payload = Map.of(
            "x", 100,
            "y", 100,
            "direction", "UP",
            "gameId", saved.getId().intValue(),
            "cityName", "ABC"
        );

        mockMvc.perform(post("/api/domino/move")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isBadRequest());
    }

    @Test
    void moveClosingLoop_marksGameCompleteAndRejectsFurtherMoves() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);
        String token = jwtService.generateToken(identity, null);

        DominoGame game = new DominoGame();
        game.setIdentity(identity);
        DominoGame saved = dominoGameRepository.save(game);

        saved.getPieces().clear();
        saved.getPieces().add(new DominoPiece(
            saved,
            new IntPoint2D(10, 10),
            DominoPiece.Direction.RIGHT,
            "TEST"
        ));
        ReflectionTestUtils.setField(saved, "beginPos", new IntPoint2D(0, 0));
        ReflectionTestUtils.setField(saved, "endPos", new IntPoint2D(1, 2));
        ReflectionTestUtils.setField(saved, "beginLetter", 'A');

        String suggestionsJson = mockMvc.perform(
                get("/api/domino/suggestions")
                    .param("text", "A")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        SuggestionResponse suggestions =
            objectMapper.readValue(suggestionsJson, SuggestionResponse.class);
        String city = suggestions.getSuggestions().get(0);

        char cityLastLetter = city.charAt(city.length() - 1);
        ReflectionTestUtils.setField(
            saved,
            "endLetter",
            cityLastLetter == 'Z' ? 'Y' : 'Z'
        );
        dominoGameRepository.save(saved);

        Map<String, Object> payload = Map.of(
            "x", 0,
            "y", 1,
            "direction", "RIGHT",
            "gameId", saved.getId(),
            "cityName", city
        );

        mockMvc.perform(post("/api/domino/move")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.complete").value(true));

        DominoGame completedGame = dominoGameRepository.findById(saved.getId()).orElseThrow();
        assertEquals(DominoGame.GameState.FINISHED, completedGame.getGameState());
        assertEquals(true, completedGame.isComplete());

        mockMvc.perform(post("/api/domino/move")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isConflict());
    }

}
