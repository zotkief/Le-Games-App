package com.example.le_games_app.repository;

import com.example.le_games_app.models.GuessCityRound;
import com.example.le_games_app.models.PlayerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface GuessCityRoundRepository extends JpaRepository<GuessCityRound, Long> {

    @Query("""
            SELECT r FROM GuessCityRound r
            JOIN FETCH r.puzzle
            WHERE r.puzzle.date = :date AND r.player = :player
            """)
    Optional<GuessCityRound> findByPuzzleDateAndPlayer(
            @Param("date") LocalDate date,
            @Param("player") PlayerIdentity player);

    @Query("""
            SELECT r FROM GuessCityRound r
            JOIN FETCH r.puzzle
            JOIN FETCH r.player
            WHERE r.id = :id
            """)
    Optional<GuessCityRound> findByIdWithPuzzleAndPlayer(@Param("id") Long id);
}
