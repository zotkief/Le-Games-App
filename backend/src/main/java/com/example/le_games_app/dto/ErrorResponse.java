package com.example.le_games_app.dto;

public record ErrorResponse(
        String code,
        String message
) {}