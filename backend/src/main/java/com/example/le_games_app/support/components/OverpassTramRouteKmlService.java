package com.example.le_games_app.support.components;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OverpassTramRouteKmlService {

    private static final URI OVERPASS_URL = URI.create("https://overpass-api.de/api/interpreter");
    private static final MediaType KML_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.google-earth.kml+xml");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OverpassTramRouteKmlService(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<byte[]> fetchTramRoutesKml(BigCity city) {
        if (city == null || city.name() == null || city.name().isBlank()) {
            throw new IllegalArgumentException("City must have an original name");
        }

        HttpRequest request = HttpRequest.newBuilder(OVERPASS_URL)
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "data=" + URLEncoder.encode(buildQuery(city.name()), StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody = new String(
                    response.body(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            throw new IllegalStateException(
                    "Overpass API returned HTTP status " + response.statusCode()
            );
        }
            System.out.println("Overpass API OK!");

            JsonNode overpassResult = objectMapper.readTree(response.body());
            if (overpassResult.has("remark")) {
                throw new IllegalStateException(
                        "Overpass API error: " + overpassResult.path("remark").asText());
            }

            byte[] kml = convertToKml(city.name(), overpassResult);
            String filename = safeFilename(city.asciiName()) + "-tram-routes.kml";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(KML_MEDIA_TYPE);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(kml);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling Overpass API", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to retrieve tram routes from Overpass API", e);
        }
    }

    private static String buildQuery(String cityName) {
        String escapedName = cityName
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        return """
                [out:json][timeout:25];
                
                area["name"="%s"]["boundary"="administrative"]->.city;
                
                rel(area.city)
                  ["type"="route"]
                  ["route"~"^(bus|trolleybus|tram)$"]
                  ["service"!~"^(long_distance|high_speed|regional)$"]
                  ->.routes;
                
                .routes out body;
                
                way(r.routes)(area.city)
                  ["railway"!~"^(rail|light_rail|subway|narrow_gauge|preserved|funicular)$"];
                out body;
                >;
                out skel qt;
                """.formatted(escapedName);
    }

    byte[] convertToKml(String cityName, JsonNode overpassResult) {
        Map<Long, List<Long>> wayNodes = new HashMap<>();
        Map<Long, String> nodeCoordinates = new HashMap<>();
        List<JsonNode> routeRelations = new ArrayList<>();

        for (JsonNode element : overpassResult.path("elements")) {
            long id = element.path("id").asLong();
            switch (element.path("type").asText()) {
                case "node" -> {
                    if (element.has("lat") && element.has("lon")) {
                        nodeCoordinates.put(id, element.path("lon").asText()
                                + "," + element.path("lat").asText() + ",0");
                    }
                }
                case "way" -> {
                    List<Long> nodes = new ArrayList<>();
                    for (JsonNode nodeId : element.path("nodes")) {
                        nodes.add(nodeId.asLong());
                    }
                    wayNodes.put(id, nodes);
                }
                case "relation" -> routeRelations.add(element);
                default -> {
                    // Ignore element types not needed to construct route geometry.
                }
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XMLStreamWriter writer = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());
            writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            writer.writeStartElement("kml");
            writer.writeDefaultNamespace("http://www.opengis.net/kml/2.2");
            writer.writeStartElement("Document");
            writeTextElement(writer, "name", cityName + " tram routes");

            for (JsonNode relation : routeRelations) {
                writeRoutePlacemark(writer, cityName, relation, wayNodes, nodeCoordinates);
            }

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.close();
            return output.toByteArray();
        } catch (IOException | XMLStreamException e) {
            throw new IllegalStateException("Failed to create KML document", e);
        }
    }

    private static void writeRoutePlacemark(
            XMLStreamWriter writer,
            String cityName,
            JsonNode relation,
            Map<Long, List<Long>> wayNodes,
            Map<Long, String> nodeCoordinates) throws XMLStreamException {
        long relationId = relation.path("id").asLong();
        String routeName = relation.path("tags").path("name").asText(
                cityName + " tram route " + relationId);

        List<List<String>> geometries = new ArrayList<>();
        for (JsonNode member : relation.path("members")) {
            if (!"way".equals(member.path("type").asText())) {
                continue;
            }

            List<String> coordinates = new ArrayList<>();
            for (Long nodeId : wayNodes.getOrDefault(member.path("ref").asLong(), List.of())) {
                String coordinate = nodeCoordinates.get(nodeId);
                if (coordinate != null) {
                    coordinates.add(coordinate);
                }
            }
            if (coordinates.size() >= 2) {
                geometries.add(coordinates);
            }
        }
        if (geometries.isEmpty()) {
            return;
        }

        writer.writeStartElement("Placemark");
        writeTextElement(writer, "name", routeName);
        writer.writeStartElement("MultiGeometry");
        for (List<String> coordinates : geometries) {
            writer.writeStartElement("LineString");
            writeTextElement(writer, "tessellate", "1");
            writeTextElement(writer, "coordinates", String.join(" ", coordinates));
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private static void writeTextElement(XMLStreamWriter writer, String element, String text)
            throws XMLStreamException {
        writer.writeStartElement(element);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }

    private static String safeFilename(String asciiName) {
        if (asciiName == null || asciiName.isBlank()) {
            return "city";
        }
        return asciiName.replaceAll("[^a-zA-Z0-9_-]", "-");
    }
}
