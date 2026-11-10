package com.lab.user.client;
import com.lab.user.entity.*;
import com.lab.user.repository.*;
import com.lab.user.seed.*;
import com.lab.user.scheduler.*;
import com.lab.user.security.*;
import com.lab.user.config.*;
import com.lab.user.controller.*;
import com.lab.user.service.*;
import com.lab.user.service.impl.*;

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
