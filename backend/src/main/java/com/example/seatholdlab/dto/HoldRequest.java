package com.example.seatholdlab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HoldRequest(
        @NotNull Long seatId,
        @NotBlank String customerName
) {}
