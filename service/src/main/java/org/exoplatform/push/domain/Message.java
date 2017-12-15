package org.exoplatform.push.domain;

public class Message {
  private String receiver;
  private String title;
  private String body;

  public Message(String receiver, String title, String body) {
    this.receiver = receiver;
    this.title = title;
    this.body = body;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String to) {
    this.receiver = to;
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