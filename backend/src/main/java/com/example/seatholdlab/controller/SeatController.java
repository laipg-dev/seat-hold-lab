package com.example.seatholdlab.controller;

import com.example.seatholdlab.dto.SeatResponse;
import com.example.seatholdlab.service.SeatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping
    public List<SeatResponse> getSeats() {
        return seatService.getSeats();
    }
}