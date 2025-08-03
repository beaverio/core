package com.beaver.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public Mono<String> pong() {
        return Mono.just("PONG");
    }
}
