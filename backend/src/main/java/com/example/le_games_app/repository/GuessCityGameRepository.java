package com.example.le_games_app.repository;

import com.example.le_games_app.models.GuessCityGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface GuessCityGameRepository extends JpaRepository<GuessCityGame, Long> {
    boolean existsByDate(LocalDate date);

    Optional<GuessCityGame> findByDate(LocalDate date);
}
