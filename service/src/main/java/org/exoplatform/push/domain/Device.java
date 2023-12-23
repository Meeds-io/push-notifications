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
package org.exoplatform.push.domain;

import org.exoplatform.commons.api.persistence.ExoEntity;

import jakarta.persistence.*;
import java.util.Date;

@Entity(name = "PushNotifsDevice")
@ExoEntity
@Table(name = "MSG_DEVICES")
@NamedQueries({
  @NamedQuery(
    name = "PushNotifsDevice.findDevicesByUsername",
    query = "SELECT d FROM PushNotifsDevice d WHERE d.username = :username"
  ),
  @NamedQuery(
    name = "PushNotifsDevice.findDevicesByToken",
    query = "SELECT d FROM PushNotifsDevice d WHERE d.token = :token"
  ),
  @NamedQuery(
    name = "PushNotifsDevice.deleteDevicesWithTokenOlderThan",
    query = "DELETE FROM PushNotifsDevice d WHERE d.registrationDate < :expirationDate"
  )

})
public class Device {

  @Id
  @SequenceGenerator(name="SEQ_MSG_DEVICES_ID", sequenceName="SEQ_MSG_DEVICES_ID")
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_MSG_DEVICES_ID")
  @Column(name = "ID")
  private long id;

  @Column(name = "TOKEN")
  private String token;

  @Column(name = "USERNAME")
  private String username;

  @Column(name = "TYPE")
  private String type;

  @Column(name = "REGISTRATION_DATE")
  private Date registrationDate;

  public Device() {
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Date registrationDate) {
    this.registrationDate = registrationDate;
  }
}
