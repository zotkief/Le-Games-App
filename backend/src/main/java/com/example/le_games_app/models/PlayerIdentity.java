package com.example.le_games_app.models;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "player_identity")
public class PlayerIdentity {

    @Id
    private UUID uid;

    // opcjonalne powiązanie z kontem
    @OneToOne
    @JoinColumn(name = "account_id", nullable = true)
    private User account;

    public PlayerIdentity() {
        this.uid = UUID.randomUUID();
    }

    public UUID getUid() {
        return uid;
    }

    public User getAccount() {
        return account;
    }

    public void setAccount(User account) {
        this.account = account;
    }
}