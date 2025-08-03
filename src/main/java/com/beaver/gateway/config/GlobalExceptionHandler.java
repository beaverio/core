package com.beaver.gateway.config;

import com.beaver.auth.exceptions.JwtTokenMalformedException;
import com.beaver.auth.exceptions.JwtTokenMissingException;
import com.beaver.gateway.dto.ErrorResponse;
import com.beaver.gateway.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Invalid input data",
                details,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public Mono<ResponseEntity<String>> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException ex, ServerWebExchange exchange) {
        // Pass through the exact error response from downstream services
        return Mono.just(ResponseEntity.status(ex.getStatusCode())
                .header("Content-Type", "application/json")
                .body(ex.getReason()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRateLimitExceeded(
            RateLimitExceededException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Rate Limit Exceeded",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Gateway Error",
                "An error occurred while processing your request",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    @ExceptionHandler(JwtTokenMissingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleJwtTokenMissing(
            JwtTokenMissingException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Required",
                "Access token is required",
                null,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    @ExceptionHandler(JwtTokenMalformedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleJwtTokenMalformed(
            JwtTokenMalformedException ex, ServerWebExchange exchange) {
        ErrorResponse errorResponse = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                "Invalid or expired access token",
                null,
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }
}
