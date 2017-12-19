package org.exoplatform.push.service;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.push.dao.DeviceDao;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.util.StringUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

public class DeviceServiceImpl implements DeviceService {

  private static final Log LOG = ExoLogger.getLogger(DeviceServiceImpl.class);

  private DeviceDao deviceDao;

  public DeviceServiceImpl(DeviceDao deviceDao) {
    this.deviceDao = deviceDao;
  }

  public void createDevice(Device device) {
    deviceDao.create(device);
    LOG.info("New device registered : username={}, token={}, type={}", device.getUsername(), StringUtil.mask(device.getToken(), 4), device.getType());
  }

  public void deleteDevice(Device device) {
    deviceDao.delete(device);
    LOG.info("Device unregistered : username={}, token={}, type={}", device.getUsername(), StringUtil.mask(device.getToken(), 4), device.getType());
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
