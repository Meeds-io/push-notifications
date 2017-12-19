package org.exoplatform.push.service.fcm;

/**
 * Configuration object for Firebase Cloud Messaging Service Account
 */
public class FCMServiceAccountConfiguration {
  private String serviceAccountId;
  private String serviceAccountClientId;
  private String serviceAccountPrivateKeyPem;
  private String serviceAccountUser;
  private String serviceAccountProjectId;
  private String serviceAccountPrivateKeyId;
  private String tokenServerEncodedUrl;

  public String getServiceAccountId() {
    return serviceAccountId;
  }

  public void setServiceAccountId(String serviceAccountId) {
    this.serviceAccountId = serviceAccountId;
  }

  public String getServiceAccountClientId() {
    return serviceAccountClientId;
  }

  public void setServiceAccountClientId(String serviceAccountClientId) {
    this.serviceAccountClientId = serviceAccountClientId;
  }

  public String getServiceAccountPrivateKeyPem() {
    return serviceAccountPrivateKeyPem;
  }

  public void setServiceAccountPrivateKeyPem(String serviceAccountPrivateKeyPem) {
    this.serviceAccountPrivateKeyPem = serviceAccountPrivateKeyPem;
  }

  public String getServiceAccountUser() {
    return serviceAccountUser;
  }

  public void setServiceAccountUser(String serviceAccountUser) {
    this.serviceAccountUser = serviceAccountUser;
  }

  public String getServiceAccountProjectId() {
    return serviceAccountProjectId;
  }

  public void setServiceAccountProjectId(String serviceAccountProjectId) {
    this.serviceAccountProjectId = serviceAccountProjectId;
  }

  public String getServiceAccountPrivateKeyId() {
    return serviceAccountPrivateKeyId;
  }

  public void setServiceAccountPrivateKeyId(String serviceAccountPrivateKeyId) {
    this.serviceAccountPrivateKeyId = serviceAccountPrivateKeyId;
  }

  public String getTokenServerEncodedUrl() {
    return tokenServerEncodedUrl;
  }

  public void setTokenServerEncodedUrl(String tokenServerEncodedUrl) {
    this.tokenServerEncodedUrl = tokenServerEncodedUrl;
  }
}
