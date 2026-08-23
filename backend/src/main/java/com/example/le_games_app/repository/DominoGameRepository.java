package com.example.le_games_app.repository;

import com.example.le_games_app.models.domino.DominoGame;
import com.example.le_games_app.models.PlayerIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DominoGameRepository extends JpaRepository<DominoGame, Long> {
    @Query("SELECT g FROM DominoGame g WHERE g.identity = :identity AND g.date = :date AND g.gameType = 'DAILY'")
    Optional<DominoGame> findDailyGameByIdentityAndDate(@Param("identity") PlayerIdentity identity, @Param("date") LocalDate date);
    
    @Query("SELECT g FROM DominoGame g WHERE g.identity = :identity AND g.gameState = 'ONGOING'")
    List<DominoGame> findOngoingGamesByIdentity(@Param("identity") PlayerIdentity identity);
}
