package com.beaver.core.client;

import com.beaver.core.dto.LoginRequest;
import com.beaver.core.dto.SignupRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user-service.url}")
    private String userServiceBaseUrl;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private WebClient getUserServiceWebClient() {
        return webClientBuilder.baseUrl(userServiceBaseUrl).build();
    }

    public Mono<UserDto> validateCredentials(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        return getUserServiceWebClient()
                .post()
                .uri("/users/internal/validate-credentials")
                .header("X-Service-Secret", gatewaySecret)
                .header("X-Source", "gateway")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody ->
                                        Mono.error(new org.springframework.web.server.ResponseStatusException(
                                                clientResponse.statusCode(), errorBody
                                        ))
                                )
                )
                .bodyToMono(UserDto.class);
    }

    public Mono<UserDto> getUserById(UUID userId) {
        return getUserServiceWebClient()
                .get()
                .uri("/users/internal/users/{userId}", userId.toString())
                .header("X-Service-Secret", gatewaySecret)
                .header("X-Source", "gateway")
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody ->
                                        Mono.error(new org.springframework.web.server.ResponseStatusException(
                                                clientResponse.statusCode(), errorBody
                                        ))
                                )
                )
                .bodyToMono(UserDto.class);
    }

    public Mono<Void> createUser(String email, String password, String name) {
        SignupRequest request = SignupRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .build();

        return getUserServiceWebClient()
                .post()
                .uri("/users/internal/users")
                .header("X-Service-Secret", gatewaySecret)
                .header("X-Source", "gateway")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody ->
                                Mono.error(new org.springframework.web.server.ResponseStatusException(
                                        clientResponse.statusCode(), errorBody
                                )))
                )
                .bodyToMono(Void.class);
    }

    @Builder
    public record UserDto(
            UUID id,
            String email,
            String name,
            boolean active,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {
    }
}