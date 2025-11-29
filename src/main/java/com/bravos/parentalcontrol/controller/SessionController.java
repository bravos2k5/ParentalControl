package com.bravos.parentalcontrol.controller;

import com.bravos.parentalcontrol.model.Session;
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
  public List<Session> listDevices() {
    return sessionService.getAllSessions();
  }

  @DeleteMapping("/{id}")
  public void deleteSession(@PathVariable(name = "id") String sessionId) {
    sessionService.deleteSession(sessionId);
  }

  @DeleteMapping
  public void deleteAllSessions() {
    sessionService.deleteAllSessions();
  }

}
