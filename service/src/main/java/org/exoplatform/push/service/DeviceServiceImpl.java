package org.exoplatform.push.service;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.push.dao.DeviceDao;
import org.exoplatform.push.domain.Device;

import java.util.List;

public class DeviceServiceImpl implements DeviceService {

  private DeviceDao deviceDao;

  public DeviceServiceImpl(DeviceDao deviceDao) {
    this.deviceDao = deviceDao;
  }

  public void createDevice(Device device) {
    deviceDao.create(device);
  }

  public void deleteDevice(Device device) {
    deviceDao.delete(device);
  }

  public Device getDeviceById(Long deviceId) {
    return deviceDao.find(deviceId);
  }

  @ExoTransactional
  public List<Device> getDevicesByUser(String username) {
    return deviceDao.findByUsername(username);
  }

  @ExoTransactional
  public Device getDeviceByToken(String token) {
    return deviceDao.findByToken(token);
  }
}
