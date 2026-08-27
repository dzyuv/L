package com.lab.common.api;
public record ApiResponse<T>(String code, String message, T data, String requestId) {
    public static <T> ApiResponse<T> success(T data, String requestId) {
        return new ApiResponse<>("SUCCESS", "操作成功", data, requestId);
    }
    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }
}
