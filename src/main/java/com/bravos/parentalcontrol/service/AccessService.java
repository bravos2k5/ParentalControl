package com.bravos.parentalcontrol.service;

public interface AccessService {

  /**
   * Grant additional time to a device
   * @param deviceId device identifier
   * @param seconds number of seconds to grant
   * @return password to unlock the device
   */
  String grantTime(String deviceId, int seconds);

  /**
   * Block device after certain time
   * @param deviceId device identifier
   * @param seconds number of seconds before blocking
   */
  void blockAfterTime(String deviceId, int seconds);

  Integer getRemainingBlockTime(String deviceId);

  /**
   * Verify access request
   * @param sessionId session identifier
   * @param code access code
   * @return number of seconds granted, null if invalid
   */
  Integer verifyAccessRequest(String sessionId, String code);

}
