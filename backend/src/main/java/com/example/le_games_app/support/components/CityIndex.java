package com.example.le_games_app.support.components;

import java.util.*;
import jakarta.annotation.*;
import java.io.BufferedReader;
import io.jsonwebtoken.io.IOException;
import java.nio.file.Files;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.io.FileReader;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

@Component
public class CityIndex {

    private final List<String> cities = new ArrayList<>();

    @PostConstruct
    public void load() {
        try (BufferedReader br = new BufferedReader(
                new FileReader("data/cities.csv"))) {

            String line;
            while ((line = br.readLine()) != null) {
                String city = line.split(";")[2].toUpperCase();
                cities.add(city);
            }

            Collections.sort(cities);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load cities", e);
        }
    }

    // -------------------------
    // EXACT MATCH (binary search)
    // -------------------------
    public boolean find(String city) {
        city = city.toUpperCase();

        int left = 0;
        int right = cities.size() - 1;

        while (left <= right) {
            int mid = (left + right) >>> 1;

            String midVal = cities.get(mid);
            int cmp = midVal.compareTo(city);

            if (cmp == 0) {
                return true;
            } else if (cmp < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return false;
    }

    // -------------------------
    // PREFIX SUGGESTIONS
    // -------------------------
    public List<String> suggest(String prefix, int limit) {
        prefix = prefix.toUpperCase();

        int idx = lowerBound(prefix);

        List<String> result = new ArrayList<>();

        for (int i = idx; i < cities.size() && result.size() < limit; i++) {
            String city = cities.get(i);

            if (!city.startsWith(prefix)) break;

            result.add(city);
        }

        return result;
    }

    // first position >= prefix
    private int lowerBound(String prefix) {
        int left = 0;
        int right = cities.size();

        while (left < right) {
            int mid = (left + right) >>> 1;

            if (cities.get(mid).compareTo(prefix) < 0) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }
}