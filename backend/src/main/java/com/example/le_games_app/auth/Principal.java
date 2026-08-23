package com.example.le_games_app.auth;

import java.util.UUID;

public class Principal {

    private UUID uid;
    private Long accountId;
    private String authType; // GUEST / ACCOUNT

    public boolean isGuest() {
        return "GUEST".equals(authType);
    }

    public boolean isAccount() {
        return "ACCOUNT".equals(authType);
    }
}