package com.lab.booking;

import com.lab.common.api.ApiResponse;
import com.lab.common.exception.BusinessException;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.api.Roles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class ApprovalTaskClient {
    private final RestClient client;
    private final String internalToken;

    ApprovalTaskClient(@Value("${services.approval.base-url:http://localhost:8084}") String baseUrl,
                       @Value("${security.internal-token:}") String token){
        client=RestClient.builder().baseUrl(baseUrl).build();
        internalToken=token;
    }

    public void create(Booking booking, ResourceRuleClient.BookingRule rule, String authorization){
        try{
            Long approverUserId = rule.approverUserId();
            String role=rule.approverRole()==null||rule.approverRole().isBlank()?(approverUserId==null?Roles.LAB_ADMIN:Roles.TEACHER):rule.approverRole();
            int totalLevels = Math.max(1, rule.approvalLevel());
            client.post().uri("/api/v1/internal/approvals/tasks")
                .header(HttpHeaders.AUTHORIZATION,authorization==null?"":authorization)
                .header(InternalServiceGuard.HEADER,internalToken)
                .body(new CreateTask(booking.id,booking.userId,booking.applicantNameSnapshot,booking.resourceId,rule.resourceTypeId(),booking.resourceNameSnapshot,booking.startTime,booking.endTime,1,totalLevels,approverUserId,role)).retrieve().toBodilessEntity();
        }catch(RestClientException exception){
            throw new BusinessException("APPROVAL_SERVICE_UNAVAILABLE","Approval service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public void closePending(Long bookingId, String reason) {
        try {
            client.post().uri("/api/v1/internal/approvals/bookings/{id}/close", bookingId)
                .header(InternalServiceGuard.HEADER, internalToken)
                .body(new ClosePending(reason)).retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw new BusinessException("APPROVAL_SERVICE_UNAVAILABLE", "Approval service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public boolean reopenCanceled(Long bookingId, java.time.LocalDateTime deadline) {
        try {
            ApiResponse<Map<String, Object>> response = client.post().uri("/api/v1/internal/approvals/bookings/{id}/reopen", bookingId)
                .header(InternalServiceGuard.HEADER, internalToken)
                .body(new ReopenPending(deadline)).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {});
            return response != null && response.data() != null;
        } catch (RestClientException exception) {
            throw new BusinessException("APPROVAL_SERVICE_UNAVAILABLE", "Approval service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record CreateTask(Long bookingId,Long applicantUserId,String applicantName,Long resourceId,Long resourceTypeId,String resourceName,
                              java.time.LocalDateTime startTime,java.time.LocalDateTime endTime,int level,int totalLevels,Long assignedUserId,String approverRole){}
    private record ClosePending(String reason) {}
    private record ReopenPending(java.time.LocalDateTime deadline) {}
}
