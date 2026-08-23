package com.example.le_games_app.security;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public String generateToken(PlayerIdentity identity, User account) {
        return Jwts.builder()
                .claim("uid", identity.getUid().toString())
                .claim("authType", account == null ? "GUEST" : "ACCOUNT")
                .claim("accountId", account != null ? account.getId() : null)
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        Jws<Claims> jwt = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return jwt.getBody();
    }
}
