package com.example.seatholdlab.repository;

import com.example.seatholdlab.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatHoldRepository
        extends JpaRepository<SeatHold, Long> {
}