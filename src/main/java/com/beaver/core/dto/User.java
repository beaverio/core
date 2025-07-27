package com.beaver.core.dto;

import lombok.Builder;

@Builder
public record User(String id, String email, String name, String password) {
}