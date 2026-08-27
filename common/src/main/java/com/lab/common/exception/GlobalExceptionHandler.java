package com.lab.common.exception;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException e, HttpServletRequest r){
        return ResponseEntity.status(e.status()).body(ApiResponse.error(e.code(), e.getMessage(), id(r)));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception e,HttpServletRequest r){
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","Request validation failed",id(r)));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception e,HttpServletRequest r){
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","Request format is invalid",id(r)));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e, HttpServletRequest r){
        return ResponseEntity.internalServerError().body(ApiResponse.error("INTERNAL_ERROR", "Internal server error", id(r)));
    }
    private String id(HttpServletRequest r){
        Object id=r.getAttribute("X-Request-Id");
        return id==null?"":id.toString();
    }
}
