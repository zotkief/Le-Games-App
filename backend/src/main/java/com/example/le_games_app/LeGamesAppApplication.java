package com.example.le_games_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeGamesAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeGamesAppApplication.class, args);
	}

}
