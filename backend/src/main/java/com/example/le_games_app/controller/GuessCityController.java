package com.example.le_games_app.controller;

import com.example.le_games_app.models.PlayerIdentity;
import com.example.le_games_app.security.PlayerIdentityAccessService;
import com.example.le_games_app.service.GuessCityRoundService;
import com.example.le_games_app.service.GuessCityRoundService.RoundResponse;
import com.example.le_games_app.support.components.BigCityIndex;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guess-city")
public class GuessCityController {

    private final GuessCityRoundService roundService;
    private final PlayerIdentityAccessService identityAccessService;
    private final BigCityIndex bigCityIndex;

    public GuessCityController(
            GuessCityRoundService roundService,
            PlayerIdentityAccessService identityAccessService,
            BigCityIndex bigCityIndex) {
        this.roundService = roundService;
        this.identityAccessService = identityAccessService;
        this.bigCityIndex = bigCityIndex;
    }

    @GetMapping("/suggestions")
    public ResponseEntity<SuggestionResponse> suggestions(@RequestParam String text) {
        return ResponseEntity.ok(new SuggestionResponse(bigCityIndex.suggest(text, 10)));
    }

    @PostMapping("/start")
    public ResponseEntity<RoundResponse> start(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        PlayerIdentity player = identityAccessService.requireIdentity(authorizationHeader);
        return ResponseEntity.ok(roundService.startToday(player));
    }

    @PostMapping("/guess")
    public ResponseEntity<RoundResponse> guess(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody GuessRequest request) {
        PlayerIdentity player = identityAccessService.requireIdentity(authorizationHeader);
        return ResponseEntity.ok(roundService.submitGuess(
                player, request.gameId(), request.cityName()));
    }

    public record GuessRequest(Long gameId, String cityName) {
    }

    public record SuggestionResponse(List<String> suggestions) {
    }
}
