package com.bravos.parentalcontrol.controller;

import com.bravos.parentalcontrol.model.TimeRequest;
import com.bravos.parentalcontrol.service.AccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/access")
public class AccessController {

  private final AccessService accessService;

  public AccessController(AccessService accessService) {
    this.accessService = accessService;
  }

  @PostMapping("/grant")
  public String grantAccess(@RequestBody TimeRequest request) {
    try {
      return accessService.grantTime(request.getDeviceId(), request.getSeconds());
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @PostMapping("/block")
  public String blockAfterTime(@RequestBody TimeRequest request) {
    try {
      accessService.blockAfterTime(request.getDeviceId(), request.getSeconds());
      return "Device will be blocked after " + request.getSeconds() + " seconds.";
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  @GetMapping("/block-time/{deviceId}")
  public String getRemainingBlockTime(@PathVariable String deviceId) {
    Integer seconds = accessService.getRemainingBlockTime(deviceId);
    if(seconds != null) {
      return "Remaining block time for device " + deviceId + ": " + seconds + " seconds.";
    } else {
      return "Device " + deviceId + " is not currently blocked.";
    }
  }

}
