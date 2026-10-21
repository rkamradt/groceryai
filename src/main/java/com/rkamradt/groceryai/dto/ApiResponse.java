package com.rkamradt.groceryai.dto;

public record ApiResponse<T>(T data, String status, String message) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, "ok", null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, "error", message);
    }
}
