package com.lab.user;

import com.lab.common.exception.BusinessException;
import com.lab.user.client.ApprovalFeignClient;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApprovalReleaseClient {
    private final ApprovalFeignClient feign;

    ApprovalReleaseClient(ApprovalFeignClient feign) {
        this.feign = feign;
    }

    public void release(Long userId) {
        try {
            feign.release(userId);
        } catch (BusinessException | FeignException exception) {
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE", "Booking service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
