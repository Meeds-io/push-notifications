package org.exoplatform.push.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.push.domain.Device;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

public class DeviceDao extends GenericDAOJPAImpl<Device, Long> {

  public List<Device> findByUsername(String username) {
    TypedQuery<Device> query = getEntityManager().createNamedQuery("PushNotifsDevice.findDevicesByUsername", Device.class)
            .setParameter("username", username);

    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return null;
    }
  }

  public Device findByToken(String token) {
    TypedQuery<Device> query = getEntityManager().createNamedQuery("PushNotifsDevice.findDevicesByToken", Device.class)
            .setParameter("token", token);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public int deleteDevicesWithTokenOlderThan(Date expirationDate) {
    Query query = getEntityManager().createNamedQuery("PushNotifsDevice.deleteDevicesWithTokenOlderThan");
    query.setParameter("expirationDate", expirationDate);
    return query.executeUpdate();
  }

}
