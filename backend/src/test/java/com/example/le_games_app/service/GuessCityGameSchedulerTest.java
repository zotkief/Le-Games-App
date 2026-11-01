package com.example.le_games_app.service;

import com.example.le_games_app.models.GuessCityGame;
import com.example.le_games_app.repository.GuessCityGameRepository;
import com.example.le_games_app.support.components.BigCity;
import com.example.le_games_app.support.components.BigCityIndex;
import com.example.le_games_app.support.components.OverpassTramRouteKmlService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GuessCityGameSchedulerTest {

    private final GuessCityGameRepository repository = mock(GuessCityGameRepository.class);
    private final BigCityIndex cityIndex = mock(BigCityIndex.class);
    private final OverpassTramRouteKmlService kmlService =
            mock(OverpassTramRouteKmlService.class);
    private final GuessCityGameScheduler scheduler =
            new GuessCityGameScheduler(repository, cityIndex, kmlService);

    @Test
    void createsGameWithDateSeededCityAndKmlFile() {
        LocalDate date = LocalDate.of(2026, 10, 19);
        BigCity city = new BigCity(
                "Łódź", "Lodz", "PL", "Poland", 670000, null, "Europe/Warsaw", "51,19");
        byte[] kml = "<kml/>".getBytes();
        when(repository.existsByDate(date)).thenReturn(false);
        when(cityIndex.getRandom(org.mockito.ArgumentMatchers.any(Random.class))).thenReturn(city);
        when(kmlService.fetchTramRoutesKml(city)).thenReturn(ResponseEntity.ok(kml));

        assertTrue(scheduler.createGameIfMissing(date));

        ArgumentCaptor<Random> randomCaptor = ArgumentCaptor.forClass(Random.class);
        verify(cityIndex).getRandom(randomCaptor.capture());
        long seed = date.getYear() * 12L * 35
                + date.getMonth().ordinal() * 35L
                + date.getDayOfMonth();
        assertEquals(new Random(seed).nextInt(), randomCaptor.getValue().nextInt());

        ArgumentCaptor<GuessCityGame> gameCaptor = ArgumentCaptor.forClass(GuessCityGame.class);
        verify(repository).save(gameCaptor.capture());
        assertEquals(date, gameCaptor.getValue().getDate());
        assertEquals("Lodz", gameCaptor.getValue().getCityAsciiName());
        assertArrayEquals(kml, gameCaptor.getValue().getKmlFile());
    }

    @Test
    void doesNotCreateAnotherGameForExistingDate() {
        LocalDate date = LocalDate.of(2026, 10, 19);
        when(repository.existsByDate(date)).thenReturn(true);

        assertFalse(scheduler.createGameIfMissing(date));

        verify(cityIndex, never()).getRandom(org.mockito.ArgumentMatchers.any());
        verify(kmlService, never()).fetchTramRoutesKml(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
