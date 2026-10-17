package com.example.le_games_app.controller;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.models.User;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import com.example.le_games_app.repository.UserRepository;
import com.example.le_games_app.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.example.le_games_app.dto.ErrorResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PlayerIdentityRepository identityRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PlayerIdentityRepository identityRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(
            summary = "User login"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logged in correctly"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(
                    schema = @Schema(
                            implementation = ErrorResponse.class
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Invalid data",
            content = @Content(
                    schema = @Schema(
                            implementation = ErrorResponse.class
                    )
            )
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        if (request.getUid() != null && !request.getUid().isBlank() && (request.getLogin() == null || request.getLogin().isBlank()) && (request.getEmail() == null || request.getEmail().isBlank())) {
            return loginWithUid(request.getUid());
        }

        if ((request.getLogin() != null && !request.getLogin().isBlank()) ||
            (request.getEmail() != null && !request.getEmail().isBlank())) {
            return loginWithCredentials(request);
        }

        return ResponseEntity.badRequest().build();
    }

    @Operation(
            summary = "Create new guest"
    )
    @PostMapping("/begin")
    public ResponseEntity<RegisterResponse> registerGuest() {
        PlayerIdentity identity = new PlayerIdentity();
        identityRepository.save(identity);
        return ResponseEntity.ok(new RegisterResponse(identity.getUid().toString()));
    }


    @Operation(
            summary = "User register"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Registered correctly"
    )
    @ApiResponse(
            responseCode = "400",
            content = @Content(
                    schema = @Schema(
                            implementation = ErrorResponse.class
                    )
            )
    )
    @Transactional
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@RequestBody RegisterRequest request) {

        if (request.getLogin() == null || request.getLogin().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No required credentials"
            );
        }

        if (userRepository.findByLogin(request.getLogin()).isPresent()
                || userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Login or email is already in use"
            );
        }

        PlayerIdentity identity;

        // --- UID handling ---
        if (request.getUid() == null || request.getUid().isBlank()) {
            identity = new PlayerIdentity();
            identity = identityRepository.save(identity);
        } else {
            UUID uid;

            try {
                uid = UUID.fromString(request.getUid());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "invalid uuid"
                );
            }

            identity = identityRepository.findById(uid)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "invalid uuid"
                    ));

            if (identity.getAccount() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Player identity is already linked to an account"
                );
            }
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setLogin(request.getLogin());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        identity.setAccount(user);
        user.setIdentity(identity);
        identityRepository.save(identity);

        return ResponseEntity.ok(
                new RegisterResponse(identity.getUid().toString())
        );
    }

    

    private ResponseEntity<AuthResponse> loginWithUid(String uidValue) {
        UUID uid;
        try {
            uid = UUID.fromString(uidValue);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<PlayerIdentity> optionalIdentity = identityRepository.findById(uid);
        if (optionalIdentity.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PlayerIdentity identity = optionalIdentity.get();
        if (identity.getAccount() != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(identity, null);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    private ResponseEntity<AuthResponse> loginWithCredentials(AuthRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Optional<User> optionalUser = Optional.empty();
        if (request.getLogin() != null && !request.getLogin().isBlank()) {
            optionalUser = userRepository.findByLogin(request.getLogin());
        }

        if (optionalUser.isEmpty() && request.getEmail() != null && !request.getEmail().isBlank()) {
            optionalUser = userRepository.findByEmail(request.getEmail());
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PlayerIdentity identity = user.getIdentity();
        if (identity == null) {
            identity = identityRepository.save(new PlayerIdentity());
            identity.setAccount(user);
            user.setIdentity(identity);
            identityRepository.save(identity);
        }

        String token = jwtService.generateToken(identity, user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    public static class AuthRequest {
        private String login;
        private String email;
        private String password;
        private String uid;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }
    }


    public static class RegisterRequest {
        private String login;
        private String email;
        private String password;
        private String uid;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUid() {
            return uid;
        }

        public void setUid(String uid) {
            this.uid = uid;
        }
    }

    public static class AuthResponse {
        private final String token;

        public AuthResponse(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }

    public static class RegisterResponse {
        private final String uid;

        public RegisterResponse(String uid) {
            this.uid = uid;
        }

        public String getUid() {
            return uid;
        }
    }
}
