package com.bravos.parentalcontrol.controller;

import com.bravos.parentalcontrol.dto.request.TimeRequest;
import com.bravos.parentalcontrol.dto.response.ApiResponse;
import com.bravos.parentalcontrol.service.AccessService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/access")
public class AccessController {
  private final AccessService accessService;

  public AccessController(AccessService accessService) {
    this.accessService = accessService;
  }

  @PostMapping("/generate-code")
  public ApiResponse<String> grantAccess(@RequestBody TimeRequest request) {
    String code = accessService.generateAccessCode(request.getDeviceId(), request.getSeconds());
    return ApiResponse.ok("Access granted", code);
  }

  @PostMapping("/grant")
  public ApiResponse<Void> grantAccessDirect(@RequestBody TimeRequest request) {
    accessService.grantAccess(request.getDeviceId(), request.getSeconds());
    return ApiResponse.ok("Access granted for " + request.getSeconds() + " seconds");
  }

  @PostMapping("/block")
  public ApiResponse<Void> blockAfterTime(@RequestBody TimeRequest request) {
    accessService.blockAfterTime(request.getDeviceId(), request.getSeconds());
    return ApiResponse.ok("Device will be blocked after " + request.getSeconds() + " seconds");
  }

  @GetMapping("/block-time/{deviceId}")
  public ApiResponse<Long> getRemainingBlockTime(@PathVariable String deviceId) {
    Long seconds = accessService.getRemainingBlockTime(deviceId);
    if (seconds != null) {
      return ApiResponse.ok("Remaining block time", seconds);
    } else {
      return ApiResponse.ok("Device is not currently blocked", null);
    }
  }

}
