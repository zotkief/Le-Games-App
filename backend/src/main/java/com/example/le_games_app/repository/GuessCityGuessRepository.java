package com.example.le_games_app.repository;

import com.example.le_games_app.models.GuessCityGuess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuessCityGuessRepository extends JpaRepository<GuessCityGuess, Long> {

    List<GuessCityGuess> findByRoundIdOrderByIdAsc(Long roundId);
}
