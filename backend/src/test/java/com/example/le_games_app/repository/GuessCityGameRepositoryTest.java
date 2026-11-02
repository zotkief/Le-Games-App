package com.example.le_games_app.repository;

import com.example.le_games_app.models.GuessCityGame;
import com.example.le_games_app.models.GuessCityGuess;
import com.example.le_games_app.models.GuessCityRound;
import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.repository.PlayerIdentityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:guess_city_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GuessCityGameRepositoryTest {

    @Autowired
    private GuessCityGameRepository repository;

    @Autowired
    private GuessCityRoundRepository roundRepository;

    @Autowired
    private GuessCityGuessRepository guessRepository;

    @Autowired
    private PlayerIdentityRepository playerRepository;

    @Test
    void persistsKmlBytesAndUniqueGameDate() {
        LocalDate date = LocalDate.of(2026, 10, 19);
        byte[] kml = "<kml>tram</kml>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        GuessCityGame saved = repository.saveAndFlush(new GuessCityGame(date, "Wroclaw", kml));

        GuessCityGame loaded = repository.findById(saved.getId()).orElseThrow();

        assertTrue(repository.existsByDate(date));
        assertArrayEquals(kml, loaded.getKmlFile());
    }

    @Test
    void persistsRoundAndItsGuessesForPlayer() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        GuessCityGame puzzle = repository.saveAndFlush(
                new GuessCityGame(date, "Wroclaw", "<kml/>".getBytes()));
        PlayerIdentity player = playerRepository.saveAndFlush(new PlayerIdentity());
        GuessCityRound round = roundRepository.saveAndFlush(new GuessCityRound(puzzle, player));

        GuessCityRound loadedRound = roundRepository
                .findByPuzzleDateAndPlayer(date, player).orElseThrow();
        guessRepository.saveAndFlush(new GuessCityGuess("Krakow", 235.0, loadedRound));

        assertTrue(loadedRound.isInProgress());
        assertEquals(1, guessRepository.findByRoundIdOrderByIdAsc(round.getId()).size());
        assertEquals(round.getId(),
                roundRepository.findByIdWithPuzzleAndPlayer(round.getId()).orElseThrow().getId());
    }
}
