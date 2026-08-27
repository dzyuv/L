package com.lab.booking;

import com.lab.common.exception.BusinessException;
import com.lab.common.api.InternalServiceGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ApprovalTaskClient {
    private final RestClient client;
    private final String internalToken;

    ApprovalTaskClient(@Value("${services.approval.base-url:http://localhost:8084}") String baseUrl,
                       @Value("${security.internal-token:}") String token){
        client=RestClient.builder().baseUrl(baseUrl).build();
        internalToken=token;
    }

    public void create(Booking booking,Long approverUserId,String authorization){
        try{
            client.post().uri("/api/v1/internal/approvals/tasks")
                .header(HttpHeaders.AUTHORIZATION,authorization==null?"":authorization)
                .header(InternalServiceGuard.HEADER,internalToken)
                .body(new CreateTask(booking.id,booking.userId,booking.applicantNameSnapshot,booking.resourceId,booking.resourceNameSnapshot,booking.startTime,booking.endTime,booking.approvalLevelSnapshot,approverUserId)).retrieve().toBodilessEntity();
        }catch(RestClientException exception){
            throw new BusinessException("APPROVAL_SERVICE_UNAVAILABLE","Approval service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record CreateTask(Long bookingId,Long applicantUserId,String applicantName,Long resourceId,String resourceName,
                              java.time.LocalDateTime startTime,java.time.LocalDateTime endTime,int level,Long assignedUserId){}
}
