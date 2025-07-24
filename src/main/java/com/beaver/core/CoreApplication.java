package com.beaver.core;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class CoreApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        new SpringApplicationBuilder(CoreApplication.class)
                .properties("spring.profiles.active=" + System.getProperty("SPRING_PROFILES_ACTIVE", "default"))
                .run(args);
        SpringApplication.run(CoreApplication.class, args);
    }
}
