package com.example.le_games_app.support.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OverpassTramRouteKmlServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OverpassTramRouteKmlService service =
            new OverpassTramRouteKmlService(objectMapper);

    @Test
    void convertsUnicodeTramRouteGeometryToKml() throws Exception {
        String overpassJson = """
                {
                  "elements": [
                    {"type":"relation","id":42,"tags":{"name":"Трамвай 1 & 2"},
                     "members":[{"type":"way","ref":7}]},
                    {"type":"way","id":7,"nodes":[10,11]},
                    {"type":"node","id":10,"lat":50.0614,"lon":19.9366},
                    {"type":"node","id":11,"lat":50.0620,"lon":19.9370}
                  ]
                }
                """;

        byte[] result = service.convertToKml(
                "Краків",
                objectMapper.readTree(overpassJson));
        String kml = new String(result, StandardCharsets.UTF_8);

        assertTrue(kml.contains("Краків tram routes"));
        assertTrue(kml.contains("Трамвай 1 &amp; 2"));
        assertTrue(kml.contains("19.9366,50.0614,0"));
        assertTrue(kml.contains("19.937,50.062,0"));
    }
}
