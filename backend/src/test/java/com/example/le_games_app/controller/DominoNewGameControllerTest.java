package com.example.le_games_app.controller;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.models.domino.DominoGame;
import com.example.le_games_app.models.domino.DominoObstacle;
import com.example.le_games_app.repository.DominoGameRepository;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.repository.UserRepository;
import com.example.le_games_app.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class DominoNewGameControllerTest {

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
    void newGamePersists_noDate() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        String token = jwtService.generateToken(identity, null);

        MvcResult result = mockMvc.perform(post("/api/domino/newgame")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new HashMap<>())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.obstacles").isArray())
        .andExpect(jsonPath("$.pieces").isArray())
        .andReturn();

        System.out.println(result.getResponse().getContentAsString());

        String resp = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(resp);

        int respObstacles = node.get("obstacles").size();
        int respPieces = node.get("pieces").size();

        List<DominoGame> games = dominoGameRepository.findAll();
        assertEquals(1, games.size());

        DominoGame saved = games.get(0);
        assertEquals(respObstacles, saved.getObstacles().size());
        assertEquals(respPieces, saved.getPieces().size());

        for (DominoObstacle o : saved.getObstacles()) {
            assertNotNull(o.getGame());
            assertEquals(saved.getId(), o.getGame().getId());
        }
    }

    @Test
    void newGameRequiresValidIdentityToken() throws Exception {
        mockMvc.perform(post("/api/domino/newgame")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>())))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/domino/newgame")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new HashMap<>())))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void newGamePersists_withDate() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        String token = jwtService.generateToken(identity, null);
        Map<String,String> payload = Map.of("date", "2026-06-04");

        MvcResult result = mockMvc.perform(post("/api/domino/newgame")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.obstacles").isArray())
        .andExpect(jsonPath("$.pieces").isArray())
        .andReturn();

        System.out.println(result.getResponse().getContentAsString());

        

        String resp = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(resp);

        int respObstacles = node.get("obstacles").size();
        int respPieces = node.get("pieces").size();

        List<DominoGame> games = dominoGameRepository.findAll();
        assertEquals(1, games.size());

        DominoGame saved = games.get(0);
        assertEquals(respObstacles, saved.getObstacles().size());
        assertEquals(respPieces, saved.getPieces().size());
    }

}
