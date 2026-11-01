package com.example.le_games_app.support.components;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class BigCityIndex {

    private final List<BigCity> cities = new ArrayList<>();

    @PostConstruct
    public void load() {
        try (BufferedReader reader = Files.newBufferedReader(
                Path.of("data/cities_500k.csv"), StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] columns = line.split(";", -1);
                if (columns.length < 20) {
                    throw new IllegalArgumentException(
                            "Expected at least 20 columns on line " + lineNumber);
                }

                cities.add(new BigCity(
                        columns[1],
                        columns[2],
                        columns[6],
                        columns[7],
                        Long.parseLong(columns[13]),
                        columns[14].isBlank() ? null : Integer.valueOf(columns[14]),
                        columns[16],
                        columns[19]
                ));
            }

            cities.sort(Comparator.comparing(city -> normalize(city.asciiName())));
        } catch (IOException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to load cities", e);
        }
    }

    public BigCity getRandom(Random rand) {
        return cities.get(rand.nextInt(cities.size()));
    }

    public Optional<BigCity> findByAsciiName(String asciiName) {
        String normalizedName = normalize(asciiName);
        return cities.stream()
                .filter(city -> normalize(city.asciiName()).equals(normalizedName))
                .findFirst();
    }

    public boolean find(String city) {
        String normalizedCity = normalize(city);

        int left = 0;
        int right = cities.size() - 1;

        while (left <= right) {
            int mid = (left + right) >>> 1;
            int comparison = normalize(cities.get(mid).asciiName()).compareTo(normalizedCity);

            if (comparison == 0) {
                return true;
            } else if (comparison < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    public List<String> suggest(String prefix, int limit) {
        String normalizedPrefix = normalize(prefix);
        int index = lowerBound(normalizedPrefix);
        List<String> result = new ArrayList<>();

        for (int i = index; i < cities.size() && result.size() < limit; i++) {
            String asciiName = cities.get(i).asciiName();
            if (!normalize(asciiName).startsWith(normalizedPrefix)) {
                break;
            }
            result.add(normalize(asciiName));
        }

        return result;
    }

    private int lowerBound(String prefix) {
        int left = 0;
        int right = cities.size();

        while (left < right) {
            int mid = (left + right) >>> 1;

            if (normalize(cities.get(mid).asciiName()).compareTo(prefix) < 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    private static String normalize(String cityName) {
        return cityName.toUpperCase(Locale.ROOT);
    }
}
