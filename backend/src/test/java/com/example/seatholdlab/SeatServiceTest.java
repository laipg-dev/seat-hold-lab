package com.example.seatholdlab;

import com.example.seatholdlab.dto.HoldRequest;
import com.example.seatholdlab.dto.PaymentRequest;
import com.example.seatholdlab.dto.SeatHoldResponse;
import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatStatus;
import com.example.seatholdlab.repository.FlightSeatRepository;
import com.example.seatholdlab.repository.SeatHoldRepository;
import com.example.seatholdlab.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SeatServiceTest {

    @Autowired
    private SeatService seatService;

    @Autowired
    private FlightSeatRepository flightSeatRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Test
    void testHoldAndPaySuccess() {
        // Seat ID 1 is seeded as '1A', AVAILABLE in V2__seed_flight_seats.sql
        HoldRequest holdRequest = new HoldRequest(1L, "Nguyen Van A");
        SeatHoldResponse holdResponse = seatService.holdSeat(holdRequest);

        assertThat(holdResponse).isNotNull();
        assertThat(holdResponse.status()).isEqualTo(HoldStatus.PENDING);
        assertThat(holdResponse.seatNumber()).isEqualTo("1A");

        // Verify seat status changed to HELD
        var seat = flightSeatRepository.findById(1L).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);

        // Pay before expiration
        PaymentRequest paymentRequest = new PaymentRequest(holdResponse.id());
        SeatHoldResponse payResponse = seatService.paySeat(paymentRequest);

        assertThat(payResponse.status()).isEqualTo(HoldStatus.PAID);
        assertThat(payResponse.paidAt()).isNotNull();

        // Verify seat status changed to SOLD
        seat = flightSeatRepository.findById(1L).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
    }

    @Test
    void testHoldAlreadyHeldSeatFails() {
        HoldRequest request1 = new HoldRequest(2L, "Customer 1");
        seatService.holdSeat(request1);

        HoldRequest request2 = new HoldRequest(2L, "Customer 2");
        assertThatThrownBy(() -> seatService.holdSeat(request2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat is not available");
    }

    @Test
    void testPayExpiredHoldFails() {
        // Create a hold manually with expired time
        var seat = flightSeatRepository.findById(3L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        var expiredHold = new com.example.seatholdlab.entity.SeatHold(
                seat,
                "Expired Customer",
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60)
        );
        var savedHold = seatHoldRepository.save(expiredHold);

        PaymentRequest paymentRequest = new PaymentRequest(savedHold.getId());
        assertThatThrownBy(() -> seatService.paySeat(paymentRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Hold has expired");

        // Verify seat released back to AVAILABLE
        seat = flightSeatRepository.findById(3L).orElseThrow();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}
