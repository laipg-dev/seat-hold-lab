package com.example.seatholdlab.service;

import com.example.seatholdlab.entity.FlightSeat;
import com.example.seatholdlab.entity.HoldStatus;
import com.example.seatholdlab.entity.SeatHold;
import com.example.seatholdlab.entity.SeatStatus;
import com.example.seatholdlab.repository.FlightSeatRepository;
import com.example.seatholdlab.repository.SeatHoldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SeatExpirationService {

    private static final Logger log = LoggerFactory.getLogger(SeatExpirationService.class);

    private final SeatHoldRepository seatHoldRepository;
    private final FlightSeatRepository flightSeatRepository;

    @Value("${app.seat-hold.simulate-error:false}")
    private boolean simulateErrorConfig;

    public SeatExpirationService(
            SeatHoldRepository seatHoldRepository,
            FlightSeatRepository flightSeatRepository) {
        this.seatHoldRepository = seatHoldRepository;
        this.flightSeatRepository = flightSeatRepository;
    }

    /**
     * Nghiệp vụ chính: Quét và giải phóng các lượt giữ đã hết hạn.
     * Nếu xảy ra lỗi giữa hai cập nhật, toàn bộ thay đổi sẽ được rollback.
     */
    @Transactional
    public int releaseExpiredHolds() {
        return releaseExpiredHoldsInternal(this.simulateErrorConfig);
    }

    /**
     * Phương thức phục vụ kiểm chứng thực hành (Mục 2.4 & 3.3):
     * Có @Transactional, cho phép chủ động kích hoạt lỗi mô phỏng để chứng minh
     * rollback.
     */
    @Transactional
    public int releaseExpiredHoldsWithTransaction(boolean simulateError) {
        return releaseExpiredHoldsInternal(simulateError);
    }

    /**
     * Phương thức phục vụ kiểm chứng thực hành (Mục 2.3 & 3.3):
     * KHÔNG có @Transactional. Ép ghi xuống DB bằng saveAndFlush.
     * Khi simulateError = true, cập nhật 1 đã lưu nhưng cập nhật 2 bị bỏ qua do
     * exception.
     */
    public int releaseExpiredHoldsWithoutTransaction(boolean simulateError) {
        Instant now = Instant.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtLessThanEqual(HoldStatus.PENDING, now);

        if (expiredHolds.isEmpty()) {
            log.debug("[SeatExpiration-NoTx] Không có lượt giữ nào hết hạn tại {}", now);
            return 0;
        }

        log.info("[SeatExpiration-NoTx] Tìm thấy {} lượt giữ hết hạn chưa xử lý", expiredHolds.size());

        for (SeatHold hold : expiredHolds) {
            log.info("[SeatExpiration-NoTx] Bắt đầu xử lý lượt giữ ID: {}, Ghế: {}",
                    hold.getId(), hold.getFlightSeat().getSeatNumber());

            // Cập nhật 1: Đổi trạng thái lượt giữ sang EXPIRED và ghi ngay xuống DB
            hold.setStatus(HoldStatus.EXPIRED);
            seatHoldRepository.saveAndFlush(hold);
            log.info("[SeatExpiration-NoTx] [Cập nhật 1 THÀNH CÔNG] SeatHold ID {} -> EXPIRED", hold.getId());

            // Gây lỗi mô phỏng ngay sau cập nhật 1
            if (simulateError) {
                log.error(
                        "[SeatExpiration-NoTx] [MÔ PHỎNG LỖI] Xảy ra sự cố sau cập nhật 1! Cập nhật 2 sẽ bị gián đoạn.");
                throw new RuntimeException("Mô phỏng lỗi hệ thống giữa 2 bước cập nhật (Không có Transaction)!");
            }

            // Cập nhật 2: Giải phóng ghế về AVAILABLE
            FlightSeat seat = hold.getFlightSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            flightSeatRepository.saveAndFlush(seat);
            log.info("[SeatExpiration-NoTx] [Cập nhật 2 THÀNH CÔNG] Ghế {} -> AVAILABLE", seat.getSeatNumber());
        }

        return expiredHolds.size();
    }

    private int releaseExpiredHoldsInternal(boolean simulateError) {
        Instant now = Instant.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findByStatusAndExpiresAtLessThanEqual(HoldStatus.PENDING, now);

        if (expiredHolds.isEmpty()) {
            log.debug("[SeatExpiration] Không có lượt giữ nào hết hạn tại {}", now);
            return 0;
        }

        log.info("[SeatExpiration] Tìm thấy {} lượt giữ PENDING đã đến hạn", expiredHolds.size());

        for (SeatHold hold : expiredHolds) {
            FlightSeat seat = hold.getFlightSeat();
            log.info("[SeatExpiration] Đang xử lý lượt giữ ID: {}, Ghế: {} ({})",
                    hold.getId(), seat.getSeatNumber(), seat.getFlightCode());

            // Cập nhật 1: Đổi trạng thái lượt giữ sang EXPIRED
            hold.setStatus(HoldStatus.EXPIRED);
            seatHoldRepository.save(hold);
            log.info("[SeatExpiration] [Bước 1] Cập nhật SeatHold ID {} -> EXPIRED", hold.getId());

            // Mô phỏng lỗi nếu cờ bật
            if (simulateError) {
                log.error("[SeatExpiration] [MÔ PHỎNG LỖI] Sự cố kích hoạt sau bước 1! Transaction sẽ rollback.");
                throw new RuntimeException("Mô phỏng lỗi hệ thống giữa 2 bước cập nhật (Có Transaction)!");
            }

            // Cập nhật 2: Đổi trạng thái ghế sang AVAILABLE
            seat.setStatus(SeatStatus.AVAILABLE);
            flightSeatRepository.save(seat);
            log.info("[SeatExpiration] [Bước 2] Cập nhật FlightSeat ID {} ({}) -> AVAILABLE",
                    seat.getId(), seat.getSeatNumber());
        }

        log.info("[SeatExpiration] Đã giải phóng thành công {} lượt giữ ghế hết hạn", expiredHolds.size());
        return expiredHolds.size();
    }
}
