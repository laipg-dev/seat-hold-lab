package com.example.seatholdlab.service;

import com.example.seatholdlab.dto.HoldRequest;
import com.example.seatholdlab.dto.PaymentRequest;
import com.example.seatholdlab.dto.SeatHoldResponse;
import com.example.seatholdlab.dto.SeatResponse;
import com.example.seatholdlab.entity.FlightSeat;
import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatHold;
import com.example.seatholdlab.entity.SeatStatus;
import com.example.seatholdlab.repository.FlightSeatRepository;
import com.example.seatholdlab.repository.SeatHoldRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SeatService {

    private final FlightSeatRepository flightSeatRepository;
    private final SeatHoldRepository seatHoldRepository;

    @Value("${app.seat-hold.timeout-seconds:30}")
    private long holdTimeoutSeconds;

    public SeatService(
            FlightSeatRepository flightSeatRepository,
            SeatHoldRepository seatHoldRepository
    ) {
        this.flightSeatRepository = flightSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
    }

    public List<SeatResponse> getSeats() {
        return flightSeatRepository.findAllByOrderByIdAsc()
                .stream()
                .map(SeatResponse::from)
                .toList();
    }

    @Transactional
    public SeatHoldResponse holdSeat(HoldRequest request) {
        FlightSeat seat = flightSeatRepository.findById(request.seatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found with id: " + request.seatId()));

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available for holding. Current status: " + seat.getStatus());
        }

        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(holdTimeoutSeconds); // Configurable hold duration (default 30s)

        SeatHold hold = new SeatHold(seat, request.customerName(), now, expiresAt);
        SeatHold savedHold = seatHoldRepository.save(hold);

        return SeatHoldResponse.from(savedHold);
    }

    @Transactional
    public SeatHoldResponse paySeat(PaymentRequest request) {
        SeatHold hold = seatHoldRepository.findById(request.holdId())
                .orElseThrow(() -> new IllegalArgumentException("Hold not found with id: " + request.holdId()));

        if (hold.getStatus() != HoldStatus.PENDING) {
            throw new IllegalStateException("Hold is not in PENDING status. Current status: " + hold.getStatus());
        }

        Instant now = Instant.now();
        if (now.isAfter(hold.getExpiresAt())) {
            hold.setStatus(HoldStatus.EXPIRED);
            FlightSeat seat = hold.getFlightSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatHoldRepository.save(hold);
            flightSeatRepository.save(seat);
            throw new IllegalStateException("Hold has expired at " + hold.getExpiresAt() + ". Payment rejected.");
        }

        hold.setStatus(HoldStatus.PAID);
        hold.setPaidAt(now);

        FlightSeat seat = hold.getFlightSeat();
        seat.setStatus(SeatStatus.SOLD);

        seatHoldRepository.save(hold);
        flightSeatRepository.save(seat);

        return SeatHoldResponse.from(hold);
    }

    public SeatHoldResponse getHoldDetails(Long holdId) {
        SeatHold hold = seatHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found with id: " + holdId));
        return SeatHoldResponse.from(hold);
    }
}
