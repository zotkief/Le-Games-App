package com.example.le_games_app.controller;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.le_games_app.models.domino.DominoGame;
import com.example.le_games_app.repository.DominoGameRepository;
import com.example.le_games_app.security.JwtService;

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
class DominoControllerTest {

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
        identityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void emptySuggestionResult() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        mockMvc.perform(get("/api/domino/suggestions")
            .param("text", "zzz")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions").isArray())
        .andExpect(jsonPath("$.suggestions.length()").value(0));
    }

    @Test
    void fullSuggestionResult() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        mockMvc.perform(get("/api/domino/suggestions")
            .param("text", "A")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions").isArray())
        .andExpect(jsonPath("$.suggestions.length()").value(10));
    }

    @Test
    void specifiedSuggestionResult() throws Exception {
        System.out.println(System.getProperty("user.dir"));
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        mockMvc.perform(get("/api/domino/suggestions")
            .param("text", "Zw")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.suggestions").isArray())
        .andExpect(jsonPath("$.suggestions.length()").value(9));
    }

    @Test
    void getUserGamesReturnsEmptyListWhenNoGames() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        String token = jwtService.generateToken(identity, null);

        mockMvc.perform(get("/api/domino/games")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameIds").isArray())
        .andExpect(jsonPath("$.gameIds.length()").value(0));
    }

    @Test
    void getUserGamesReturnsOnlyOngoingGames() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        DominoGame game1 = new DominoGame();
        game1.setIdentity(identity);
        dominoGameRepository.save(game1);

        DominoGame game2 = new DominoGame();
        game2.setIdentity(identity);
        dominoGameRepository.save(game2);

        String token = jwtService.generateToken(identity, null);

        mockMvc.perform(get("/api/domino/games")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gameIds").isArray())
        .andExpect(jsonPath("$.gameIds.length()").value(2));
    }

    @Test
    void getGameStateReturnsGameData() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        DominoGame game = new DominoGame();
        game.setIdentity(identity);
        DominoGame savedGame = dominoGameRepository.save(game);

        String token = jwtService.generateToken(identity, null);
        mockMvc.perform(get("/api/domino/game/" + savedGame.getId())
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.obstacles").isArray())
        .andExpect(jsonPath("$.pieces").isArray());
    }

    @Test
    void getGameStateReturnsNotFoundForInvalidGameId() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);
        String token = jwtService.generateToken(identity, null);

        mockMvc.perform(get("/api/domino/game/99999")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
    }

    @Test
    void getGameStateDoesNotExposeAnotherPlayersGame() throws Exception {
        PlayerIdentity owner = new PlayerIdentity();
        identityRepository.save(owner);
        PlayerIdentity requester = new PlayerIdentity();
        identityRepository.save(requester);

        DominoGame game = new DominoGame();
        game.setIdentity(owner);
        DominoGame savedGame = dominoGameRepository.save(game);
        String token = jwtService.generateToken(requester, null);

        mockMvc.perform(get("/api/domino/game/" + savedGame.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    public static class SuggestionRequest {
        public String text;

        public SuggestionRequest(String searchText)
        {
            text = searchText;
        }
    }
}
