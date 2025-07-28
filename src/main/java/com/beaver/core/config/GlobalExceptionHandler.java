package com.beaver.core.config;

import com.beaver.core.exception.AuthenticationFailedException;
import com.beaver.core.exception.JwtTokenIncorrectStructureException;
import com.beaver.core.exception.JwtTokenMalformedException;
import com.beaver.core.exception.JwtTokenMissingException;
import com.beaver.core.dto.ErrorResponseDto;
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
    public Mono<ResponseEntity<ErrorResponseDto>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponseDto errorResponse = ErrorResponseDto.of(
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

    @ExceptionHandler(AuthenticationFailedException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleAuthenticationFailed(
            AuthenticationFailedException ex, ServerWebExchange exchange) {
        ErrorResponseDto errorResponse = ErrorResponseDto.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    @ExceptionHandler({JwtTokenMalformedException.class, JwtTokenIncorrectStructureException.class})
    public Mono<ResponseEntity<ErrorResponseDto>> handleJwtTokenErrors(
            Exception ex, ServerWebExchange exchange) {
        ErrorResponseDto errorResponse = ErrorResponseDto.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid Token",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    @ExceptionHandler(JwtTokenMissingException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleJwtTokenMissing(
            JwtTokenMissingException ex, ServerWebExchange exchange) {
        ErrorResponseDto errorResponse = ErrorResponseDto.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Token Missing",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleIllegalArgument(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        ErrorResponseDto errorResponse = ErrorResponseDto.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Request",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponseDto>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        ErrorResponseDto errorResponse = ErrorResponseDto.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                exchange.getRequest().getPath().value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
}
