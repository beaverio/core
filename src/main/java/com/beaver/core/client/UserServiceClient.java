package com.beaver.core.client;

import com.beaver.core.dto.LoginRequest;
import com.beaver.core.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;
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

    public Mono<Map<String, Object>> validateCredentials(String email, String password) {
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
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<Map<String, Object>> validateCredentialsWithWorkspaces(String email, String password) {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        return getUserServiceWebClient()
                .post()
                .uri("/users/internal/validate-credentials-with-workspaces")
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
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<Map<String, Object>> getUserById(UUID userId) {
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
                .bodyToMono(new ParameterizedTypeReference<>() {});
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

    public Mono<Map<String, Object>> validateWorkspaceAccess(UUID userId, UUID workspaceId) {
        return getUserServiceWebClient()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/internal/validate-workspace-access")
                        .queryParam("userId", userId)
                        .queryParam("workspaceId", workspaceId)
                        .build())
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
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<Map<String, Object>> updateEmail(UUID userId, String newEmail, String currentPassword) {
        Map<String, String> request = Map.of(
                "email", newEmail,
                "currentPassword", currentPassword
        );

        return getUserServiceWebClient()
                .patch()
                .uri("/users/internal/users/{userId}/email", userId.toString())
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
                .bodyToMono(new ParameterizedTypeReference<>() {});
    }

    public Mono<Void> updatePassword(UUID userId, String currentPassword, String newPassword) {
        Map<String, String> request = Map.of(
                "currentPassword", currentPassword,
                "newPassword", newPassword
        );

        return getUserServiceWebClient()
                .patch()
                .uri("/users/internal/users/{userId}/password", userId.toString())
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
                .bodyToMono(Void.class);
    }
}