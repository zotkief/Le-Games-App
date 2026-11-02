package com.example.le_games_app.models;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_identity")
public class PlayerIdentity {

    @Id
    private UUID uid;

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