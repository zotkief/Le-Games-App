package com.example.le_games_app.models;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String login;

    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @OneToOne(mappedBy = "account")
    private PlayerIdentity identity;

    public User() {}

    public Long getId() {
        return id;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public PlayerIdentity getIdentity() {
        return identity;
    }

    public void setIdentity(PlayerIdentity identity) {
        this.identity = identity;
    }
}