package com.beaver.core.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class BaseDto {

    private UUID id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}