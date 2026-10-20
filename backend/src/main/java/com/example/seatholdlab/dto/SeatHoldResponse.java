package com.example.seatholdlab.dto;

import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatHold;

import java.time.Instant;

public record SeatHoldResponse(
        Long id,
        Long seatId,
        String flightCode,
        String seatNumber,
        String customerName,
        HoldStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt
) {
    public static SeatHoldResponse from(SeatHold hold) {
        return new SeatHoldResponse(
                hold.getId(),
                hold.getFlightSeat().getId(),
                hold.getFlightSeat().getFlightCode(),
                hold.getFlightSeat().getSeatNumber(),
                hold.getCustomerName(),
                hold.getStatus(),
                hold.getCreatedAt(),
                hold.getExpiresAt(),
                hold.getPaidAt()
        );
    }
}
