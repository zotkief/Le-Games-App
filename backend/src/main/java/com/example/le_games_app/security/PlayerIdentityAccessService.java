package com.example.le_games_app.security;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayerIdentityAccessService {

    private final PlayerIdentityRepository identityRepository;
    private final JwtService jwtService;

    public PlayerIdentityAccessService(
            PlayerIdentityRepository identityRepository,
            JwtService jwtService) {
        this.identityRepository = identityRepository;
        this.jwtService = jwtService;
    }

    public PlayerIdentity requireIdentity(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw unauthorized();
        }

        try {
            Claims claims = jwtService.parseToken(authorizationHeader.substring(7));
            Object uidClaim = claims.get("uid");
            if (!(uidClaim instanceof String uidValue)) {
                throw unauthorized();
            }
            UUID uid = UUID.fromString(uidValue);
            PlayerIdentity identity = identityRepository.findById(uid)
                    .orElseThrow(PlayerIdentityAccessService::unauthorized);

            boolean isAccount = identity.getAccount() != null;
            String expectedAuthType = isAccount ? "ACCOUNT" : "GUEST";
            Object authTypeClaim = claims.get("authType");
            Object accountIdClaim = claims.get("accountId");

            if (!expectedAuthType.equals(authTypeClaim)
                    || (isAccount && !identity.getAccount().getId().toString().equals(
                            String.valueOf(accountIdClaim)))
                    || (!isAccount && accountIdClaim != null)) {
                throw unauthorized();
            }

            return identity;
        } catch (JwtException | IllegalArgumentException exception) {
            throw unauthorized();
        }
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired credentials");
    }
}
