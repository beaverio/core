package com.beaver.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Gateway is running!");
    }
}
