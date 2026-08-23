package com.example.le_games_app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidUuidException extends ResponseStatusException {
    public InvalidUuidException() {
        super(HttpStatus.BAD_REQUEST, "invalid uuid");
    }
}