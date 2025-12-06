package com.bravos.parentalcontrol.controller;

import com.bravos.parentalcontrol.dto.response.ApiResponse;
import com.bravos.parentalcontrol.entity.Session;
import com.bravos.parentalcontrol.service.SessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sessions")
public class SessionController {
  private final SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @GetMapping
  public ApiResponse<List<Session>> listDevices() {
    return ApiResponse.ok(sessionService.getAllSessions());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteSession(@PathVariable(name = "id") String sessionId) {
    sessionService.deleteSession(sessionId);
    return ApiResponse.ok("Session deleted successfully");
  }

  @DeleteMapping
  public ApiResponse<Void> deleteAllSessions() {
    sessionService.deleteAllSessions();
    return ApiResponse.ok("All sessions deleted successfully");
  }
}
