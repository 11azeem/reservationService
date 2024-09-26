package com.hms.reservationservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Tag(name = "Reservation Service", description = "Reservation Service APIs")
@RestController
@RequestMapping("/api/v1")
public class MainRestController {

    @Autowired
    private final WebClient hotelManagementServiceClient;

    @Autowired
    public MainRestController(WebClient hotelManagementServiceClient) {
        this.hotelManagementServiceClient = hotelManagementServiceClient;
    }

    @GetMapping("/getAvailableRooms")
    public ResponseEntity<JsonNode> getAvailableRooms() {

        Mono<JsonNode> responseMono = hotelManagementServiceClient
                .get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseJsonResponse);

        return ResponseEntity.ok(responseMono.block());
    }

    private JsonNode parseJsonResponse(String jsonString) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON response", e);
        }
    }
}
