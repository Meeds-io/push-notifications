package org.exoplatform.push.service;

import org.exoplatform.push.domain.Device;

import java.util.List;

public interface DeviceService {

  void createDevice(Device device);

  void deleteDevice(Device device);

  Device getDeviceById(Long deviceId);

  List<Device> getDevicesByUser(String username);

  Device getDeviceByToken(String token);

  void deleteDevicesWithExpiredToken();

}
