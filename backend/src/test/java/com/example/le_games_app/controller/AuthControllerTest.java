package com.example.le_games_app.controller;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.models.User;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

import static org.assertj.core.api.Assertions.assertThat;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerIdentityRepository identityRepository;

    @BeforeEach
    void setUp() {
        identityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void whenLoginWithUidAndIdentityExists_thenReturnsJwtToken() throws Exception {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, identity.getUid().toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    void whenLoginWithInvalidUidFormat_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, "invalid-uid"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenLoginWithUidNotFound_thenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, UUID.randomUUID().toString()))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void whenLoginWithLoginAndPasswordAndExistingIdentity_thenReturnsJwtToken() throws Exception {
        User user = new User();
        user.setLogin("player1");
        user.setEmail("player1@example.com");
        user.setPasswordHash("secret");
        userRepository.save(user);

        PlayerIdentity identity = new PlayerIdentity();
        identity.setAccount(user);
        identityRepository.save(identity);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("player1", null, "secret", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();
        assertThat(identityRepository.findById(identity.getUid())).isPresent();
    }

    @Test
    void whenLoginWithEmailAndPasswordAndNoIdentity_thenCreatesIdentityAndReturnsJwtToken() throws Exception {
        User user = new User();
        user.setLogin("player2");
        user.setEmail("player2@example.com");
        user.setPasswordHash("secret");
        userRepository.save(user);

        long beforeCount = identityRepository.count();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, "player2@example.com", "secret", null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();
        assertThat(identityRepository.count()).isEqualTo(beforeCount + 1);
        assertThat(identityRepository.findAll().stream().anyMatch(i -> i.getAccount() != null)).isTrue();
    }

    @Test
    void whenLoginWithInvalidPassword_thenReturnsUnauthorized() throws Exception {
        User user = new User();
        user.setLogin("player1");
        user.setEmail("player1@example.com");
        user.setPasswordHash("secret");
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("player1", null, "wrong-password", null))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void whenLoginWithoutCredentials_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, null))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void whenLoginWithLoginWithoutPassword_thenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("player1", null, null, null))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void beginAndLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/begin"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.has("uid")).isTrue();

        String uid = json.get("uid").asText();
        assertThat(uid).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void registerAndLogin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("login", "email@email.email", "password", null))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.has("uid")).isTrue();

        String uid = json.get("uid").asText();
        assertThat(uid).isNotBlank();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(null, null, null, uid))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void registerBadCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("login", null, "password", null))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void registerBadUid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("login", "email@email.email", "password", "999"))))
            .andExpect(status().isBadRequest());
    }

    private static class LoginRequest {
        public String login;
        public String email;
        public String password;
        public String uid;

        public LoginRequest(String login, String email, String password, String uid) {
            this.login = login;
            this.email = email;
            this.password = password;
            this.uid = uid;
        }
    }
}
