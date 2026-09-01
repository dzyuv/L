package com.lab.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BookingDecisionClient {
    private final RestClient client;
    private final ObjectMapper json;
    private final String internalToken;

    BookingDecisionClient(@Value("${services.booking.base-url:http://localhost:8083}") String baseUrl,
                          @Value("${security.internal-token:}") String token, ObjectMapper objectMapper){
        client=RestClient.builder().baseUrl(baseUrl).build();
        json=objectMapper;
        internalToken=token;
    }

    public record BookingSnapshot(String status, Integer approvalLevelSnapshot, java.time.LocalDateTime approvalDeadline) {}

    public BookingSnapshot submit(Long bookingId,String status,String comment,int level,int totalLevels,String authorization){
        try{
            ApiResponse<BookingSnapshot> response=client.post().uri("/api/v1/internal/bookings/{id}/approval-decision",bookingId)
                .header(HttpHeaders.AUTHORIZATION,authorization==null?"":authorization)
                .header(InternalServiceGuard.HEADER,internalToken)
                .body(new Decision(status,comment,level,totalLevels)).retrieve()
                .body(new ParameterizedTypeReference<ApiResponse<BookingSnapshot>>(){});
            return response==null?null:response.data();
        }catch(BusinessException exception){
            throw exception;
        }catch(RestClientResponseException exception){
            throw downstreamError(exception);
        }catch(RestClientException exception){
            throw new BusinessException("BOOKING_SERVICE_UNAVAILABLE","Booking service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BusinessException downstreamError(RestClientResponseException exception){
        try{
            ApiResponse<?> error=json.readValue(exception.getResponseBodyAsString(),ApiResponse.class);
            if(error.code()!=null && error.message()!=null){
                return new BusinessException(error.code(),error.message(),HttpStatus.valueOf(exception.getStatusCode().value()));
            }
        }catch(Exception ignored){
            // Keep the response contract stable when a downstream proxy returns a non-API body.
        }
        return new BusinessException("BOOKING_SERVICE_UNAVAILABLE","Booking service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
    }

    private record Decision(String status,String comment,int level,int totalLevels){}
}
