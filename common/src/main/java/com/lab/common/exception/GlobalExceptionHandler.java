package com.lab.common.exception;
import com.lab.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","提交的信息不完整或格式不正确",id(r)));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception e,HttpServletRequest r){
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_ARGUMENT","请求格式不正确，请检查填写内容",id(r)));
    }
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiResponse<Void>> missing(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("NOT_FOUND", "请求的数据不存在或已被删除", id(r)));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> methodNotAllowed(Exception e, HttpServletRequest r) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResponse.error("INVALID_ACTION", "不支持该操作", id(r)));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e, HttpServletRequest r){
        log.error("Unhandled request failure: {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.internalServerError().body(ApiResponse.error("INTERNAL_ERROR", "系统处理失败，请稍后重试", id(r)));
    }
    private String id(HttpServletRequest r){
        Object id=r.getAttribute("X-Request-Id");
        return id==null?"":id.toString();
    }
}
