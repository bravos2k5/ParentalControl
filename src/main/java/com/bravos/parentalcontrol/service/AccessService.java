package com.bravos.parentalcontrol.service;

import com.bravos.parentalcontrol.entity.Session;
import com.bravos.parentalcontrol.util.DateTimeHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AccessService {
  private final RedisTemplate<Object, Object> redisTemplate;
  private final SessionService sessionService;

  public AccessService(RedisTemplate<Object, Object> redisTemplate, SessionService sessionService) {
    this.redisTemplate = redisTemplate;
    this.sessionService = sessionService;
  }

  /**
   * Grant access to the device for a specified number of seconds.
   * @param deviceId device identifier
   * @param seconds number of seconds to grant access
   */
  public void grantAccess(String deviceId, int seconds) {
    Session session = sessionService.getSessionByDeviceId(deviceId);
    sessionService.sendMessageToSession(session.getId(), "GRANTED:" + seconds);
  }

  /**
   * Generate a time-limited access code for the device.
   * @param deviceId device identifier
   * @param seconds validity duration in seconds
   * @return the generated access code
   */
  public String generateAccessCode(String deviceId, int seconds) {
    Session session = sessionService.getSessionByDeviceId(deviceId);
    if (session == null) {
      throw new IllegalArgumentException("No active session for device: " + deviceId);
    }
    String code = String.valueOf((int) (Math.random() * 900000) + 100000);
    String key = "time_grant:" + session.getId() + ":" + code;
    Long expiration = DateTimeHelper.currentTimeMillis() + seconds * 1000L;
    redisTemplate.opsForValue().set(key, expiration, Duration.ofSeconds(seconds));
    return code;
  }

  /**
   * Block the device after a specified number of seconds.
   * @param deviceId device identifier
   * @param seconds number of seconds after which to block the device
   */
  public void blockAfterTime(String deviceId, int seconds) {
    Session session = sessionService.getSessionByDeviceId(deviceId);
    if (session == null) {
      throw new IllegalArgumentException("No active session for device: " + deviceId);
    }
    String key = "block_device:" + deviceId;
    long lockTimestamp = DateTimeHelper.currentTimeMillis() + seconds * 1000L;
    redisTemplate.opsForValue().set(key, lockTimestamp, Duration.ofSeconds(seconds));
    sessionService.sendMessageToSession(session.getId(), "BLOCK:" + seconds);
  }

  public Long getRemainingBlockTime(String deviceId) {
    var value = redisTemplate.opsForValue().get("block_device:" + deviceId);
    if (value != null) {
      long remainingMillis = (Long) value - DateTimeHelper.currentTimeMillis();
      if (remainingMillis > 0) return remainingMillis / 1000;
    }
    return null;
  }

  public Integer verifyAccessRequest(String sessionId, String code) {
    var value = redisTemplate.opsForValue().get("time_grant:" + sessionId + ":" + code);
    if (value != null) {
      long remainingMillis = (Long) value - DateTimeHelper.currentTimeMillis();
      if (remainingMillis > 0) {
        return (int) (remainingMillis / 1000);
      }
    }
    return null;
  }
}
