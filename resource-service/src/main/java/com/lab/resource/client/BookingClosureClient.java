package com.lab.resource.client;

import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import com.lab.resource.client.BookingClosureFeignClient;
import com.lab.resource.client.BookingClosureFeignClient.CloseForMaintenance;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class BookingClosureClient {
    private final BookingClosureFeignClient feign;

    BookingClosureClient(BookingClosureFeignClient feign) {
        this.feign = feign;
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "cancelFallback")
    @Retry(name = "bookingService")
    public int cancelOverlapping(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        return postCount(feign.closeForMaintenance(new CloseForMaintenance(resourceId, startTime, endTime, reason)),
                "cancelledCount");
    }

    @CircuitBreaker(name = "bookingService", fallbackMethod = "restoreFallback")
    @Retry(name = "bookingService")
    public int restoreOverlapping(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        return postCount(feign.restoreForMaintenance(new CloseForMaintenance(resourceId, startTime, endTime, null)),
                "restoredCount");
    }

    private int postCount(ApiResponse<Map<String, Object>> response, String countKey) {
        if (response == null || response.data() == null) return 0;
        Object count = response.data().get(countKey);
        return count instanceof Number number ? number.intValue() : 0;
    }

    private int cancelFallback(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, String reason, Throwable throwable) {
        throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private int restoreFallback(Long resourceId, LocalDateTime startTime, LocalDateTime endTime, Throwable throwable) {
        throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
