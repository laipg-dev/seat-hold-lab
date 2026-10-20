package com.example.seatholdlab.service;

import com.example.seatholdlab.dto.SeatResponse;
import com.example.seatholdlab.repository.FlightSeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final FlightSeatRepository flightSeatRepository;

    public SeatService(FlightSeatRepository flightSeatRepository) {
        this.flightSeatRepository = flightSeatRepository;
    }

    public List<SeatResponse> getSeats() {
        return flightSeatRepository.findAllByOrderByIdAsc()
                .stream()
                .map(SeatResponse::from)
                .toList();
    }
}