package com.lab.common.exception;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException e, HttpServletRequest r){
        return ResponseEntity.status(e.status()).body(ApiResponse.error(e.code(), e.getMessage(), id(r)));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e, HttpServletRequest r){
        return ResponseEntity.internalServerError().body(ApiResponse.error("INTERNAL_ERROR", e.getMessage(), id(r)));
    }
    private String id(HttpServletRequest r){
        Object id=r.getAttribute("X-Request-Id");
        return id==null?"":id.toString();
    }
}
