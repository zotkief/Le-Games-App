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
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class GuessCityRoundService {

    private final GuessCityGameRepository gameRepository;
    private final GuessCityRoundRepository roundRepository;
    private final GuessCityGuessRepository guessRepository;
    private final BigCityIndex cityIndex;

    public GuessCityRoundService(
            GuessCityGameRepository gameRepository,
            GuessCityRoundRepository roundRepository,
            GuessCityGuessRepository guessRepository,
            BigCityIndex cityIndex) {
        this.gameRepository = gameRepository;
        this.roundRepository = roundRepository;
        this.guessRepository = guessRepository;
        this.cityIndex = cityIndex;
    }

    @Transactional
    public RoundResponse startToday(PlayerIdentity player) {
        LocalDate today = LocalDate.now();
        GuessCityGame dailyGame = gameRepository.findByDate(today)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Today's GuessCityGame is not available"));

        GuessCityRound round = roundRepository.findByPuzzleDateAndPlayer(today, player)
                .orElseGet(() -> roundRepository.save(new GuessCityRound(dailyGame, player)));
        return toResponse(round);
    }

    @Transactional
    public RoundResponse submitGuess(PlayerIdentity player, Long roundId, String cityName) {
        if (cityName == null || cityName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "City name is required");
        }

        GuessCityRound round = roundRepository.findByIdWithPuzzleAndPlayer(roundId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "GuessCity round not found"));
        if (!round.getPlayer().getUid().equals(player.getUid())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "GuessCity round not found");
        }
        if (!round.isInProgress()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is already finished");
        }

        String normalizedGuess = normalize(cityName);
        String normalizedAnswer = normalize(round.getPuzzle().getCityAsciiName());
        if (normalizedGuess.equals(normalizedAnswer)) {
            guessRepository.save(new GuessCityGuess(cityName, 0.0, round));
            round.finish();
            return toResponse(round);
        }

        BigCity guessedCity = cityIndex.findByAsciiName(cityName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "City not found"));
        BigCity answerCity = cityIndex.findByAsciiName(round.getPuzzle().getCityAsciiName())
                .orElseThrow(() -> new IllegalStateException(
                        "The daily game's city is missing from the city index"));

        double distanceKm = distanceKm(guessedCity.coordinates(), answerCity.coordinates());
        guessRepository.save(new GuessCityGuess(cityName, distanceKm, round));
        return toResponse(round);
    }

    private RoundResponse toResponse(GuessCityRound round) {
        List<GuessResponse> guesses = guessRepository.findByRoundIdOrderByIdAsc(round.getId())
                .stream()
                .map(guess -> new GuessResponse(guess.getCityName(), guess.getDistanceKm()))
                .toList();
        String cityAsciiName = round.isInProgress()
                ? null
                : round.getPuzzle().getCityAsciiName();

        return new RoundResponse(
                round.getId(),
                round.getPuzzle().getDate(),
                round.isInProgress(),
                cityAsciiName,
                Base64.getEncoder().encodeToString(round.getPuzzle().getKmlFile()),
                guesses
        );
    }

    private static double distanceKm(String firstCoordinates, String secondCoordinates) {
        double[] first = parseCoordinates(firstCoordinates);
        double[] second = parseCoordinates(secondCoordinates);

        double firstLatitude = Math.toRadians(first[0]);
        double secondLatitude = Math.toRadians(second[0]);
        double latitudeDelta = secondLatitude - firstLatitude;
        double longitudeDelta = Math.toRadians(second[1] - first[1]);
        double haversine = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(firstLatitude) * Math.cos(secondLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6371.0088 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private static double[] parseCoordinates(String coordinates) {
        String[] parts = coordinates.split(",", -1);
        if (parts.length != 2) {
            throw new IllegalStateException("Invalid city coordinates: " + coordinates);
        }
        try {
            double latitude = Double.parseDouble(parts[0].trim());
            double longitude = Double.parseDouble(parts[1].trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new NumberFormatException("Coordinates are out of range");
            }
            return new double[]{latitude, longitude};
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid city coordinates: " + coordinates, exception);
        }
    }

    private static String normalize(String cityName) {
        return cityName.trim().toUpperCase(Locale.ROOT);
    }

    public record RoundResponse(
            Long id,
            LocalDate date,
            boolean inProgress,
            String cityAsciiName,
            String kmlFileBase64,
            List<GuessResponse> guesses
    ) {
    }

    public record GuessResponse(String cityName, double distanceKm) {
    }
}
