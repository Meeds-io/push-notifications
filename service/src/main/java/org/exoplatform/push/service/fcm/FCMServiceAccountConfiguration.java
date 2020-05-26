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
