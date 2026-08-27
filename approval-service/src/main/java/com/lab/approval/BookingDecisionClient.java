package com.lab.approval;

import com.lab.common.exception.BusinessException;
import com.lab.common.api.InternalServiceGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BookingDecisionClient {
    private final RestClient client;
    private final String internalToken;

    BookingDecisionClient(@Value("${services.booking.base-url:http://localhost:8083}") String baseUrl,
                          @Value("${security.internal-token:}") String token){
        client=RestClient.builder().baseUrl(baseUrl).build();
        internalToken=token;
    }

    public void submit(Long bookingId,String status,String authorization){
        try{
            client.post().uri("/api/v1/internal/bookings/{id}/approval-decision",bookingId)
                .header(HttpHeaders.AUTHORIZATION,authorization==null?"":authorization)
                .header(InternalServiceGuard.HEADER,internalToken)
                .body(new Decision(status)).retrieve().toBodilessEntity();
        }catch(RestClientException exception){
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE","Booking service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private record Decision(String status){}
}
