package com.bravos.parentalcontrol.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
public class ApiResponse<T> {
  boolean success;
  String message;
  T data;

  public static <T> ApiResponse<T> ok(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .data(data)
        .build();
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .message(message)
        .data(data)
        .build();
  }

  public static ApiResponse<Void> ok(String message) {
    return ApiResponse.<Void>builder()
        .success(true)
        .message(message)
        .build();
  }

  public static ApiResponse<Void> error(String message) {
    return ApiResponse.<Void>builder()
        .success(false)
        .message(message)
        .build();
  }
}
