package com.example.seatholdlab.dto;

import com.example.seatholdlab.entity.FlightSeat;
import com.example.seatholdlab.entity.SeatStatus;

public record SeatResponse(
        Long id,
        String flightCode,
        String seatNumber,
        SeatStatus status
) {
    public static SeatResponse from(FlightSeat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getFlightCode(),
                seat.getSeatNumber(),
                seat.getStatus()
        );
    }
}