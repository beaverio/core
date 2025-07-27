package com.beaver.core.temp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TempControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void tempEndpoint_ShouldReturnStaticMessage() {
        webTestClient.get()
                .uri("/temp")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("This is a temporary endpoint for testing purposes.");
    }
}