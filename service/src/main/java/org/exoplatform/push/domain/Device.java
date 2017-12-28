package org.exoplatform.push.domain;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;
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
