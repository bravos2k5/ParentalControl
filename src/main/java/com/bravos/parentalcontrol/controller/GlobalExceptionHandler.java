package com.bravos.parentalcontrol.controller;

import com.bravos.parentalcontrol.dto.response.ApiResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<@NonNull ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("IllegalArgumentException: {}", e.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<@NonNull ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
    log.warn("IllegalStateException: {}", e.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<@NonNull ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
    log.error("RuntimeException: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("An unexpected error occurred"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<@NonNull ApiResponse<Void>> handleException(Exception e) {
    log.error("Exception: {}", e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("An unexpected error occurred"));
  }

}
