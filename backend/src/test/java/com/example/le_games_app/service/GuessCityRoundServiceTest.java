package com.example.le_games_app.service;

import com.example.le_games_app.models.GuessCityGame;
import com.example.le_games_app.models.GuessCityGuess;
import com.example.le_games_app.models.GuessCityRound;
import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.repository.GuessCityGameRepository;
import com.example.le_games_app.repository.GuessCityGuessRepository;
import com.example.le_games_app.repository.GuessCityRoundRepository;
import com.example.le_games_app.support.components.BigCity;
import com.example.le_games_app.support.components.BigCityIndex;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuessCityRoundServiceTest {

    private final GuessCityGameRepository gameRepository = mock(GuessCityGameRepository.class);
    private final GuessCityRoundRepository roundRepository = mock(GuessCityRoundRepository.class);
    private final GuessCityGuessRepository guessRepository = mock(GuessCityGuessRepository.class);
    private final BigCityIndex cityIndex = mock(BigCityIndex.class);
    private final GuessCityRoundService service = new GuessCityRoundService(
            gameRepository, roundRepository, guessRepository, cityIndex);

    @Test
    void savesWrongGuessAndReturnsDistanceAndKml() {
        PlayerIdentity player = new PlayerIdentity();
        GuessCityRound round = newRound(player, "Wroclaw");
        when(roundRepository.findByIdWithPuzzleAndPlayer(1L)).thenReturn(Optional.of(round));
        when(cityIndex.findByAsciiName("Krakow")).thenReturn(Optional.of(city("Krakow", "50.0614, 19.9366")));
        when(cityIndex.findByAsciiName("Wroclaw")).thenReturn(Optional.of(city("Wroclaw", "51.1079, 17.0385")));
        when(guessRepository.findByRoundIdOrderByIdAsc(1L)).thenReturn(
                List.of(new GuessCityGuess("Krakow", 235.0, round)));

        GuessCityRoundService.RoundResponse response = service.submitGuess(player, 1L, "Krakow");

        assertTrue(response.inProgress());
        assertEquals(1, response.guesses().size());
        assertTrue(response.guesses().get(0).distanceKm() > 0);
        assertEquals("a21s", response.kmlFileBase64());
        ArgumentCaptor<GuessCityGuess> guessCaptor = ArgumentCaptor.forClass(GuessCityGuess.class);
        verify(guessRepository).save(guessCaptor.capture());
        assertTrue(guessCaptor.getValue().getDistanceKm() > 230);
    }

    @Test
    void correctGuessFinishesRoundAndDoesNotRevealAnswerUntilThen() {
        PlayerIdentity player = new PlayerIdentity();
        GuessCityRound round = newRound(player, "Wroclaw");
        when(roundRepository.findByIdWithPuzzleAndPlayer(1L)).thenReturn(Optional.of(round));
        when(guessRepository.findByRoundIdOrderByIdAsc(1L)).thenReturn(
                List.of(new GuessCityGuess("wRoClAw", 0.0, round)));

        GuessCityRoundService.RoundResponse response = service.submitGuess(player, 1L, "wRoClAw");

        assertFalse(response.inProgress());
        assertEquals("Wroclaw", response.cityAsciiName());
        assertEquals(1, response.guesses().size());
        assertEquals(0.0, response.guesses().get(0).distanceKm());
        verify(roundRepository, never()).save(any());
        verify(guessRepository).save(any(GuessCityGuess.class));
    }

    @Test
    void rejectsGuessForAnotherPlayersRound() {
        PlayerIdentity owner = new PlayerIdentity();
        GuessCityRound round = newRound(owner, "Wroclaw");

        when(roundRepository.findByIdWithPuzzleAndPlayer(1L)).thenReturn(Optional.of(round));

        assertThrows(ResponseStatusException.class,
                () -> service.submitGuess(new PlayerIdentity(), 1L, "Krakow"));
        verify(guessRepository, never()).save(any());
    }

    @Test
    void rejectsGuessWhenRoundAlreadyFinished() {
        PlayerIdentity player = new PlayerIdentity();
        GuessCityRound round = newRound(player, "Wroclaw");
        round.finish();
        when(roundRepository.findByIdWithPuzzleAndPlayer(1L)).thenReturn(Optional.of(round));

        assertThrows(ResponseStatusException.class,
                () -> service.submitGuess(player, 1L, "Krakow"));
        verify(guessRepository, never()).save(any());
    }

    private static GuessCityRound newRound(PlayerIdentity player, String answer) {
        GuessCityGame dailyGame = new GuessCityGame(
                LocalDate.now(), answer, "kml".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new GuessCityRound(dailyGame, player) {
            @Override
            public Long getId() {
                return 1L;
            }
        };
    }

    private static BigCity city(String name, String coordinates) {
        return new BigCity(name, name, "PL", "Poland", 100000, null, "Europe/Warsaw", coordinates);
    }
}
