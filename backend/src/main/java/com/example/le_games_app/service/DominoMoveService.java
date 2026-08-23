package com.example.le_games_app.service;

import org.springframework.stereotype.Service;
import com.example.le_games_app.support.components.CityIndex;

@Service
public class DominoMoveService {

    private final CityIndex cityIndex;

    public DominoMoveService(CityIndex cityIndex) {
        this.cityIndex = cityIndex;
    }
}