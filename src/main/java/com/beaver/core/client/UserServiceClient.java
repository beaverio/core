package com.beaver.core.client;

import com.beaver.core.dto.LoginRequest;
import com.beaver.core.dto.SignupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user-service.url}")
    private String userServiceBaseUrl;

    @Value("{gateway.secret}")
    private String gatewaySecret;

    private WebClient getUserServiceWebClient() {
        return webClientBuilder.baseUrl(userServiceBaseUrl).build();
    }

    public Mono<UserCredentialsResponse> validateCredentials(String email, String password) {
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
                .bodyToMono(UserCredentialsResponse.class)
                .onErrorReturn(UserCredentialsResponse.invalid());
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
                .bodyToMono(Void.class);
    }

    public record UserCredentialsResponse(
            boolean isValid,
            String userId,
            String email,
            String name,
            boolean isActive
    ) {
        public static UserCredentialsResponse invalid() {
            return new UserCredentialsResponse(false, null, null, null, false);
        }

        public static UserCredentialsResponse valid(String userId, String email, String name, boolean isActive) {
            return new UserCredentialsResponse(true, userId, email, name, isActive);
        }
    }
}