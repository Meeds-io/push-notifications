package org.exoplatform.push.service;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.dao.DeviceDao;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.util.StringUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.time.Instant;
import java.util.Date;
import java.util.List;

public class DeviceServiceImpl implements DeviceService {

  private static final Log LOG = ExoLogger.getLogger(DeviceServiceImpl.class);

  private DeviceDao deviceDao;

  // Token expiration time - defaults to 60 days
  private int tokenExpirationSeconds = 5184000;

  public DeviceServiceImpl(InitParams initParams, DeviceDao deviceDao) {
    if(initParams != null) {
      ValueParam tokenExpirationTimeParam = initParams.getValueParam("tokenExpirationTime");
      if(tokenExpirationTimeParam != null) {
        try {
          tokenExpirationSeconds = Integer.parseInt(tokenExpirationTimeParam.getValue());
        } catch(NumberFormatException e) {
          LOG.error("Push Notifications - Token expiration time is not a valid number ("
                  + tokenExpirationTimeParam.getValue() + "), using default value " + tokenExpirationSeconds, e);
        }
      }
    }

    this.deviceDao = deviceDao;
  }

  @ExoTransactional
  public void saveDevice(Device device) {
    if(device.getRegistrationDate() == null) {
      device.setRegistrationDate(new Date());
    }
    Device existingDevice = getDeviceByToken(device.getToken());
    if(existingDevice != null) {
      if(device.getUsername().equals(existingDevice.getUsername())) {
        existingDevice.setType(device.getType());
        existingDevice.setRegistrationDate(device.getRegistrationDate());
        deviceDao.update(existingDevice);
        LOG.info("Device updated : username={}, token={}, type={}", device.getUsername(), StringUtil.mask(device.getToken(), 4), device.getType());
      } else {
        throw new RuntimeException("Token already registered for another user !");
      }
    } else {
      deviceDao.create(device);
      LOG.info("New device registered : username={}, token={}, type={}", device.getUsername(), StringUtil.mask(device.getToken(), 4), device.getType());
    }
  }

  @ExoTransactional
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

  @ExoTransactional
  public void deleteDevicesWithExpiredToken() {
    long expirationTimeMillis = tokenExpirationSeconds * 1000;

    Instant instant = Instant.now();
    long currentTimeStamp = instant.toEpochMilli();
    Instant expirationInstant = Instant.ofEpochMilli(currentTimeStamp - expirationTimeMillis);

    int nbDeleted = deviceDao.deleteDevicesWithTokenOlderThan(Date.from(expirationInstant));

    LOG.debug("{} devices deleted", nbDeleted);
  }
}
