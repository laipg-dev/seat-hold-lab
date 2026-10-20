package com.example.seatholdlab.repository;

import com.example.seatholdlab.entity.FlightSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightSeatRepository
        extends JpaRepository<FlightSeat, Long> {

    List<FlightSeat> findAllByOrderByIdAsc();
}