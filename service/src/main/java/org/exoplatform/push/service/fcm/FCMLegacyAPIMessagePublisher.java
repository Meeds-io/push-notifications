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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.domain.Message;
import org.exoplatform.push.service.MessagePublisher;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class FCMLegacyAPIMessagePublisher implements MessagePublisher {

  private static final Log LOG = ExoLogger.getLogger(FCMLegacyAPIMessagePublisher.class);

  private HttpClient httpClient;

  private String serverKey = null;

  // How long (in seconds) the message should be kept in FCM storage if the device is offline
  private Integer fcmMessageExpirationTime = null;

  public FCMLegacyAPIMessagePublisher(InitParams initParams) {
    this(initParams, HttpClientBuilder.create().build());
  }

  public FCMLegacyAPIMessagePublisher(InitParams initParams, HttpClient httpClient) {
    if(initParams != null) {
      ValueParam serverKeyValueParam = initParams.getValueParam("serverKey");
      if(serverKeyValueParam != null) {
        serverKey = serverKeyValueParam.getValue();
      }
      if(StringUtils.isBlank(serverKey)) {
        LOG.error("Push notifications - Firebase Cloud Messaging serverKey is mandatory, please configure it with exo.push.fcm.serverKey property.");
      }

      // FCM message expiration
      ValueParam fcmMessageExpirationTimeValueParam = initParams.getValueParam("messageExpirationTime");
      if(fcmMessageExpirationTimeValueParam != null && StringUtils.isNotBlank(fcmMessageExpirationTimeValueParam.getValue())) {
        try {
          fcmMessageExpirationTime = Integer.parseInt(fcmMessageExpirationTimeValueParam.getValue());
        } catch (NumberFormatException e) {
          LOG.error("Push Notifications - FCM message expiration time is not a valid number ("
                  + fcmMessageExpirationTimeValueParam.getValue() + "), using default value from FCM", e);
        }
      }
    }

    this.httpClient = httpClient;
  }

  @Override
  public void send(Message message) throws Exception {
    if(StringUtils.isBlank(serverKey)) {
      return;
    }

    HttpPost post = new HttpPost("https://fcm.googleapis.com/fcm/send");
    post.setHeader(HttpHeaders.AUTHORIZATION, "key=" + serverKey);
    post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

    StringBuilder requestBody = new StringBuilder()
            .append("{")
            .append("  \"to\":\"").append(message.getToken()).append("\",");
    if(fcmMessageExpirationTime != null) {
      requestBody.append("  \"time_to_live\":\"").append(fcmMessageExpirationTime).append("\",");
    }
    requestBody.append("  \"notification\": {")
            .append("    \"title\": \"").append(message.getTitle()).append("\",")
            .append("    \"body\": \"").append(message.getBody()).append("\"")
            .append("  }")
            .append("}")
            .toString();

    post.setEntity(new ByteArrayEntity(requestBody.toString().getBytes()));

    httpClient.execute(post);
  }

}
