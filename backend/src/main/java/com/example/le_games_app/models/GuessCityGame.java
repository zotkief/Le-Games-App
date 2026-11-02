package com.example.le_games_app.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
        name = "guess_city_games",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_guess_city_game_date",
                columnNames = "game_date"
        )
)
public class GuessCityGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_date", nullable = false)
    private LocalDate date;

    @Column(name = "city_ascii_name", nullable = false)
    private String cityAsciiName;

    @Column(name = "kml_file", nullable = false, columnDefinition = "bytea")
    private byte[] kmlFile;

    protected GuessCityGame() {
    }

    public GuessCityGame(LocalDate date, String cityAsciiName, byte[] kmlFile) {
        this.date = date;
        this.cityAsciiName = cityAsciiName;
        this.kmlFile = kmlFile;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getCityAsciiName() {
        return cityAsciiName;
    }

    public byte[] getKmlFile() {
        return kmlFile;
    }
}
