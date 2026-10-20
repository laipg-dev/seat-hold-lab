package com.example.seatholdlab.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull Long holdId
) {}
