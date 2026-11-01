package com.example.le_games_app.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "guess_city_guesses")
public class GuessCityGuess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guess_city_round_id", nullable = false)
    private GuessCityRound round;

    protected GuessCityGuess() {
    }

    public GuessCityGuess(String cityName, double distanceKm, GuessCityRound round) {
        this.cityName = cityName;
        this.distanceKm = distanceKm;
        this.round = round;
    }

    public Long getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public GuessCityRound getRound() {
        return round;
    }
}
