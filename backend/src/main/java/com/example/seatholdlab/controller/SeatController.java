package com.example.seatholdlab.controller;

import com.example.seatholdlab.dto.HoldRequest;
import com.example.seatholdlab.dto.PaymentRequest;
import com.example.seatholdlab.dto.SeatHoldResponse;
import com.example.seatholdlab.dto.SeatResponse;
import com.example.seatholdlab.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/hold")
    public ResponseEntity<SeatHoldResponse> holdSeat(@Valid @RequestBody HoldRequest request) {
        SeatHoldResponse response = seatService.holdSeat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/pay")
    public ResponseEntity<SeatHoldResponse> paySeat(@Valid @RequestBody PaymentRequest request) {
        SeatHoldResponse response = seatService.paySeat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/holds/{id}")
    public ResponseEntity<SeatHoldResponse> getHoldDetails(@PathVariable Long id) {
        SeatHoldResponse response = seatService.getHoldDetails(id);
        return ResponseEntity.ok(response);
    }
}
