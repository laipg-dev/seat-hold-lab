package com.example.seatholdlab;

import com.example.seatholdlab.entity.FlightSeat;
import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatHold;
import com.example.seatholdlab.entity.SeatStatus;
import com.example.seatholdlab.repository.FlightSeatRepository;
import com.example.seatholdlab.repository.SeatHoldRepository;
import com.example.seatholdlab.service.SeatExpirationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SeatExpirationServiceTest {

    @Autowired
    private SeatExpirationService seatExpirationService;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private FlightSeatRepository flightSeatRepository;

    @BeforeEach
    void setUp() {
        seatHoldRepository.deleteAll();
        var seats = flightSeatRepository.findAll();
        for (var seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            flightSeatRepository.save(seat);
        }
    }

    @Test
    @DisplayName("Lượt giữ chưa hết hạn -> Bỏ qua, giữ nguyên trạng thái PENDING và HELD")
    void testDoNotReleaseUnexpiredHold() {
        FlightSeat seat = flightSeatRepository.findById(1L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Unexpired Customer", now, now.plusSeconds(60));
        seatHoldRepository.save(hold);

        int released = seatExpirationService.releaseExpiredHolds();

        assertThat(released).isEqualTo(0);
        SeatHold reloadedHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(reloadedHold.getStatus()).isEqualTo(HoldStatus.PENDING);
        FlightSeat reloadedSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(reloadedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    @DisplayName("Lượt giữ hết hạn chưa thanh toán -> Chuyển sang EXPIRED và giải phóng ghế AVAILABLE")
    void testReleaseExpiredUnpaidHold() {
        FlightSeat seat = flightSeatRepository.findById(2L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Expired Customer", now.minusSeconds(100), now.minusSeconds(10));
        seatHoldRepository.save(hold);

        int released = seatExpirationService.releaseExpiredHolds();

        assertThat(released).isEqualTo(1);
        SeatHold reloadedHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(reloadedHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);
        FlightSeat reloadedSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(reloadedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Lượt giữ đã thanh toán (PAID) -> Bỏ qua, giữ nguyên PAID và SOLD")
    void testDoNotReleasePaidHold() {
        FlightSeat seat = flightSeatRepository.findById(3L).orElseThrow();
        seat.setStatus(SeatStatus.SOLD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Paid Customer", now.minusSeconds(120), now.minusSeconds(60));
        hold.setStatus(HoldStatus.PAID);
        hold.setPaidAt(now.minusSeconds(70));
        seatHoldRepository.save(hold);

        int released = seatExpirationService.releaseExpiredHolds();

        assertThat(released).isEqualTo(0);
        SeatHold reloadedHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(reloadedHold.getStatus()).isEqualTo(HoldStatus.PAID);
        FlightSeat reloadedSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(reloadedSeat.getStatus()).isEqualTo(SeatStatus.SOLD);
    }

    @Test
    @DisplayName("Chạy lại sau khi đã xử lý -> Không xử lý lại (Idempotent)")
    void testIdempotentRunAfterRelease() {
        FlightSeat seat = flightSeatRepository.findById(4L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Customer Repeat", now.minusSeconds(100), now.minusSeconds(10));
        seatHoldRepository.save(hold);

        // Lần chạy 1: giải phóng thành công
        int firstRun = seatExpirationService.releaseExpiredHolds();
        assertThat(firstRun).isEqualTo(1);

        // Lần chạy 2: không còn bản ghi PENDING nào hết hạn
        int secondRun = seatExpirationService.releaseExpiredHolds();
        assertThat(secondRun).isEqualTo(0);
    }

    @Test
    @DisplayName("Không có Transaction + Lỗi sau cập nhật 1 -> Dữ liệu cập nhật dở dang")
    void testPartialUpdateWithoutTransaction() {
        FlightSeat seat = flightSeatRepository.findById(5L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Customer Inconsistent", now.minusSeconds(100), now.minusSeconds(10));
        seatHoldRepository.save(hold);

        // Gọi method KHÔNG có Transaction với simulateError = true
        assertThatThrownBy(() -> seatExpirationService.releaseExpiredHoldsWithoutTransaction(true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mô phỏng lỗi hệ thống");

        // Kiểm chứng dữ liệu dở dang (Inconsistent):
        // SeatHold đã chuyển sang EXPIRED (cập nhật 1 đã lưu)
        SeatHold reloadedHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(reloadedHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);

        // NHƯNG FlightSeat vẫn là HELD (cập nhật 2 chưa thực hiện và cập nhật 1 không bị rollback)
        FlightSeat reloadedSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(reloadedSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    @DisplayName("Có Transaction + Lỗi sau cập nhật 1 -> Toàn bộ thay đổi được Rollback")
    void testRollbackWithTransaction() {
        FlightSeat seat = flightSeatRepository.findById(6L).orElseThrow();
        seat.setStatus(SeatStatus.HELD);
        flightSeatRepository.save(seat);

        Instant now = Instant.now();
        SeatHold hold = new SeatHold(seat, "Customer Rollback", now.minusSeconds(100), now.minusSeconds(10));
        seatHoldRepository.save(hold);

        // Gọi method CÓ @Transactional với simulateError = true
        assertThatThrownBy(() -> seatExpirationService.releaseExpiredHoldsWithTransaction(true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mô phỏng lỗi hệ thống");

        // Kiểm chứng tính nguyên tử (Atomicity): Cả 2 cập nhật đều được rollback
        // SeatHold vẫn là PENDING
        SeatHold reloadedHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(reloadedHold.getStatus()).isEqualTo(HoldStatus.PENDING);

        // FlightSeat vẫn là HELD
        FlightSeat reloadedSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(reloadedSeat.getStatus()).isEqualTo(SeatStatus.HELD);

        // Bây giờ gọi lại với simulateError = false -> Cả 2 cập nhật thành công!
        int released = seatExpirationService.releaseExpiredHoldsWithTransaction(false);
        assertThat(released).isEqualTo(1);

        SeatHold successHold = seatHoldRepository.findById(hold.getId()).orElseThrow();
        assertThat(successHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);

        FlightSeat successSeat = flightSeatRepository.findById(seat.getId()).orElseThrow();
        assertThat(successSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
}
