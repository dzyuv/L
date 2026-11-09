package com.lab.common.exception;
import com.lab.common.api.ApiResponse;
import com.lab.common.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException e, HttpServletRequest r){
        return ResponseEntity.status(e.status()).body(ApiResponse.error(e.code(), UserMessageLocalizer.resolve(e.code(), e.getMessage()), id(r)));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception e,HttpServletRequest r){
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","Submitted data is incomplete or invalid",id(r)));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception e,HttpServletRequest r){
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","Request format is invalid",id(r)));
    }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiResponse<Void>> missing(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", "The requested data does not exist or has been removed", id(r)));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> methodNotAllowed(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error("INVALID_ACTION", "This operation is not supported", id(r)));
    }
    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<ApiResponse<Void>> upstreamUnavailable(ResourceAccessException e, HttpServletRequest r) {
        log.warn("Downstream service is unavailable: {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("SERVICE_UNAVAILABLE", "Service is temporarily unavailable; please retry later", id(r)));
    }

    @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e, HttpServletRequest r){
        log.error("Unhandled request failure: {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(ApiResponse.error("INTERNAL_ERROR", "Request processing failed; please retry later", id(r)));
    }
    private String id(HttpServletRequest r){
        Object id=r.getAttribute(RequestIdFilter.HEADER);
        return id==null?"":id.toString();
    }
}
