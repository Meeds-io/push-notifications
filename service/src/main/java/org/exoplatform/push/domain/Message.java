package org.exoplatform.push.domain;

public class Message {
  private String receiver;
  private String token;
  private String deviceType;
  private String title;
  private String body;

  public Message(String receiver, String token, String deviceType, String title, String body) {
    this.receiver = receiver;
    this.token = token;
    this.deviceType = deviceType;
    this.title = title;
    this.body = body;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String to) {
    this.receiver = to;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}