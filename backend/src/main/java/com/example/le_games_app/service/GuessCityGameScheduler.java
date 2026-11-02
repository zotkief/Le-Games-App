package com.example.le_games_app.service;

import com.example.le_games_app.models.GuessCityGame;
import com.example.le_games_app.repository.GuessCityGameRepository;
import com.example.le_games_app.support.components.BigCity;
import com.example.le_games_app.support.components.BigCityIndex;
import com.example.le_games_app.support.components.OverpassTramRouteKmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDate;
import java.util.Random;

@Component
@ConditionalOnProperty(
        name = "guess-city.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class GuessCityGameScheduler {

    private static final Logger logger = LoggerFactory.getLogger(GuessCityGameScheduler.class);

    private final GuessCityGameRepository gameRepository;
    private final BigCityIndex bigCityIndex;
    private final OverpassTramRouteKmlService kmlService;

    public GuessCityGameScheduler(
            GuessCityGameRepository gameRepository,
            BigCityIndex bigCityIndex,
            OverpassTramRouteKmlService kmlService) {
        this.gameRepository = gameRepository;
        this.bigCityIndex = bigCityIndex;
        this.kmlService = kmlService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createGameAtStartup() {
        createTodaysGameWithLogging();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void createGameHourly() {
        createTodaysGameWithLogging();
    }

    public synchronized boolean createGameIfMissing(LocalDate date) {
        if (gameRepository.existsByDate(date)) {
            return false;
        }

        long seed = date.getYear() * 12L * 35
                + date.getMonth().ordinal() * 35L
                + date.getDayOfMonth();
        BigCity city = bigCityIndex.getRandom(new Random(seed));

        ResponseEntity<byte[]> kmlResponse = kmlService.fetchTramRoutesKml(city);
        byte[] kmlFile = kmlResponse.getBody();
        if (!kmlResponse.getStatusCode().is2xxSuccessful() || kmlFile == null) {
            throw new IllegalStateException("Overpass did not return a KML file for " + city.name());
        }

        gameRepository.save(new GuessCityGame(date, city.asciiName(), kmlFile));
        logger.info("Created GuessCityGame for {} with city {}", date, city.asciiName());
        return true;
    }

    private void createTodaysGameWithLogging() {
        LocalDate today = LocalDate.now();
        try {
            createGameIfMissing(today);
        } catch (RuntimeException exception) {
            logger.error("Failed to create GuessCityGame for {}", today, exception);
        }
    }
}
