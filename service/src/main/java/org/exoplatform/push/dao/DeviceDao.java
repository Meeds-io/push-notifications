/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.push.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
import org.exoplatform.push.domain.Device;

import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
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
