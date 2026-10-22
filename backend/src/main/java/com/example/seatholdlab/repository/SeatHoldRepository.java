package com.example.seatholdlab.repository;

import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatHold;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
public interface SeatHoldRepository
        extends JpaRepository<SeatHold, Long> {

    @EntityGraph(attributePaths = {"flightSeat"})
    List<SeatHold> findByStatusAndExpiresAtLessThanEqual(HoldStatus status, Instant expiresAt);
}