package com.example.seatholdlab.scheduler;

import com.example.seatholdlab.service.SeatExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "app.seat-hold.scheduling.enabled", matchIfMissing = true)
public class SeatHoldScheduler {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldScheduler.class);

    private final SeatExpirationService seatExpirationService;

    public SeatHoldScheduler(SeatExpirationService seatExpirationService) {
        this.seatExpirationService = seatExpirationService;
    }

    /**
     * Tác vụ quét định kỳ các lượt giữ chỗ đã hết hạn.
     * Sử dụng fixedDelay để đảm bảo lần quét tiếp theo chỉ bắt đầu sau khi lần quét
     * trước đã hoàn tất,
     * ngăn chặn chồng lấn thread (overlapping execution).
     * 
     * Gọi sang bean SeatExpirationService có @Transactional để kích hoạt Spring
     * Transaction Proxy.
     */
    @Scheduled(fixedDelayString = "${app.seat-hold.scan-interval-ms:5000}", initialDelayString = "${app.seat-hold.initial-delay-ms:5000}")
    public void scheduleSeatExpirationScan() {
        log.info("[Scheduler] [TICK] Bắt đầu quét kiểm tra lượt giữ hết hạn lúc {}", Instant.now());
        try {
            int releasedCount = seatExpirationService.releaseExpiredHolds();
            // int releasedCount =
            // seatExpirationService.releaseExpiredHoldsWithoutTransaction(true);
            // int releasedCount =
            // seatExpirationService.releaseExpiredHoldsWithTransaction(true);

            if (releasedCount > 0) {
                log.info("[Scheduler] [HOÀN TẤT] Đã giải phóng thành công {} lượt giữ hết hạn.", releasedCount);
            } else {
                log.info("[Scheduler] [HOÀN TẤT] Không có lượt giữ nào hết hạn trong lần quét này.");
            }
        } catch (Exception e) {
            log.error("[Scheduler] [LỖI] Tác vụ quét gặp lỗi: {}. Transaction đã được xử lý tương ứng.",
                    e.getMessage());
        }
    }
}
