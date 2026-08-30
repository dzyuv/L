package com.lab.booking;

import com.lab.common.api.ApiResponse;
import com.lab.common.api.InternalServiceGuard;
import com.lab.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.LocalDateTime;

@Component
public class ResourceRuleClient {
    public record BookingRule(String resourceName,int capacity,int slotMinutes,int maxDurationMinutes,boolean needCheckin,int approvalLevel,Long approverUserId,String approverRole){}
    private final RestClient client;
    private final ObjectMapper json;
    private final String internalToken;

    ResourceRuleClient(@Value("${services.resource.base-url:http://localhost:8082}") String baseUrl,
                       @Value("${security.internal-token:}") String token,ObjectMapper objectMapper){
        client=RestClient.builder().baseUrl(baseUrl).build();
        json=objectMapper;
        internalToken=token;
    }

    public BookingRule getRule(Long resourceId,LocalDateTime startTime,LocalDateTime endTime,int participants,String authorization){
        try{
            ApiResponse<BookingRule> response=client.get()
                .uri(builder->builder.path("/api/v1/internal/resources/{id}/booking-rule")
                    .queryParam("startTime",startTime).queryParam("endTime",endTime)
                    .queryParam("participants",participants).build(resourceId))
                .header(HttpHeaders.AUTHORIZATION,authorization==null?"":authorization)
                .header(InternalServiceGuard.HEADER,internalToken)
                .retrieve().body(new ParameterizedTypeReference<ApiResponse<BookingRule>>(){});
            if(response==null || response.data()==null){
                throw new BusinessException("RESOURCE_RULE_UNAVAILABLE","Resource rule response is empty",HttpStatus.SERVICE_UNAVAILABLE);
            }
            return response.data();
        }catch(BusinessException e){
            throw e;
        }catch(RestClientResponseException e){
            throw downstreamError(e);
        }catch(RestClientException e){
            throw new BusinessException("RESOURCE_SERVICE_UNAVAILABLE","Resource service is unavailable",HttpStatus.SERVICE_UNAVAILABLE);
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
        return new BusinessException("RESOURCE_RULE_REJECTED","Resource rule validation rejected this booking",HttpStatus.valueOf(exception.getStatusCode().value()));
    }
}
