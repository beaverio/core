package com.beaver.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/temp")
public class TempController {
    
    @GetMapping
    public String getTempMessage() {
        return "Hello from TempController! This is a temporary endpoint for testing purposes.";
    }
}