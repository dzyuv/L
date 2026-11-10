package com.lab.booking.client;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.booking.client.ResourceRuleFeignClient;
import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ResourceRuleClient {
    public record BookingRule(String resourceName, Long resourceTypeId, int capacity, int slotMinutes,
                              int maxDurationMinutes, boolean needCheckin, int approvalLevel,
                              Long approverUserId, String approverRole) {}

    private final ResourceRuleFeignClient feign;
    private final ObjectMapper json;

    ResourceRuleClient(ResourceRuleFeignClient feign, ObjectMapper json) {
        this.feign = feign;
        this.json = json;
    }

    @CircuitBreaker(name = "resourceService", fallbackMethod = "ruleFallback")
    @Retry(name = "resourceService")
    public BookingRule getRule(Long resourceId, LocalDateTime startTime, LocalDateTime endTime,
                               int participants, Long applicantUserId, String authorization) {
        try {
            ApiResponse<BookingRule> response = feign.bookingRule(resourceId,
                    startTime.toString(), endTime.toString(), participants, applicantUserId,
                    authorization == null ? "" : authorization);
            if (response == null || response.data() == null) {
                throw new BusinessException("RESOURCE_RULE_UNAVAILABLE", "Resource rule response is empty", HttpStatus.SERVICE_UNAVAILABLE);
            }
            return response.data();
        } catch (BusinessException exception) {
            throw exception;
        } catch (FeignException exception) {
            throw downstreamError(exception);
        }
    }

    private BookingRule ruleFallback(Long resourceId, LocalDateTime startTime, LocalDateTime endTime,
                                     int participants, Long applicantUserId, String authorization,
                                     Throwable throwable) {
        throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private BusinessException downstreamError(FeignException exception) {
        try {
            ApiResponse<?> error = json.readValue(exception.contentUTF8(), ApiResponse.class);
            if (error.code() != null && error.message() != null) {
                return new BusinessException(error.code(), error.message(), HttpStatus.valueOf(exception.status()));
            }
        } catch (Exception ignored) {
            // Keep the response contract stable when a downstream proxy returns a non-API body.
        }
        if (exception.status() > 0) {
            return new BusinessException("RESOURCE_RULE_REJECTED", "Resource rule validation rejected this booking",
                    HttpStatus.valueOf(exception.status()));
        }
        return new BusinessException("RESOURCE_SERVICE_UNAVAILABLE", "Resource service is unavailable",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
