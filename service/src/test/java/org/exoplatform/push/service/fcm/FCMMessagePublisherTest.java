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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.domain.Message;
import org.exoplatform.push.exception.InvalidTokenException;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FCMMessagePublisherTest {

  @Mock
  private ResourceBundleService resourceBundleService;

  @Mock
  private CloseableHttpClient httpClient;

  @Mock
  private CloseableHttpResponse httpResponse;

  @Mock
  private WebNotificationService webNotificationService;

  @Before
  public void setup() {
    ResourceBundle resourceBundle = new ResourceBundle() {
      @Override
      protected Object handleGetObject(String key) {
        return "inline image";
      }

      @Override
      public Enumeration<String> getKeys() {
        return Collections.enumeration(Collections.singleton("Notification.push.label.InlineImage"));
      }
    };
    when(resourceBundleService.getResourceBundle(eq("locale.portlet.notification.PushNotifications"), any(Locale.class))).thenReturn(resourceBundle);
    when(webNotificationService.getNumberOnBadge(anyString())).thenReturn(5);
  }

  @Test
  public void shouldNotSendMessageWhenInitParamsAreNull() throws Exception {
    // Given
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(null, resourceBundleService, webNotificationService, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldNotSendMessageWhenNoConfigFilePathParam() throws Exception {
    // Given
    InitParams initParams = new InitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldNotSendMessageWhenNoConfigFile() throws Exception {
    // Given
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue("fake.json");
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldSendMessageWhenConfigFileExistsAndResponseOK() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body", "http://notification.url/target"));
    messagePublisher.send(new Message("mary", "token2", "ios", "My Notification Title", "My Notification Body", "http://notification.url/target"));

    // Then
    verify(httpClient, times(2)).execute(reqArgs.capture());

    List<HttpPost> httpUriRequests = reqArgs.getAllValues();
    assertNotNull(httpUriRequests);

    HttpPost httpUriRequest = httpUriRequests.get(0);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertFalse(jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject data = message.getJSONObject("data");
    assertEquals("My Notification Title", data.getString("title"));
    assertEquals("My Notification Body", data.getString("body"));
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token1", message.getString("token"));
    assertFalse(message.has("android"));
    assertFalse(message.has("ios"));

    httpUriRequest = httpUriRequests.get(1);
    assertNotNull(httpUriRequest);
    body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    jsonMessage = new JSONObject(body);
    assertFalse(jsonMessage.getBoolean("validate_only"));
    message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("notification");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Body", notification.getString("body"));
    data = message.getJSONObject("data");
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token2", message.getString("token"));
    assertFalse(message.has("android"));
    assertTrue(message.has("apns"));
    JSONObject apns = message.getJSONObject("apns");
    assertTrue(apns.has("payload"));
    JSONObject payload = apns.getJSONObject("payload");
    assertTrue(payload.has("aps"));
    JSONObject aps = payload.getJSONObject("aps");
    assertTrue(aps.has("badge"));
    assertEquals(5, aps.getInt("badge"));
  }

  @Test
  public void shouldSendSanitizedHTMLMessageWhenConfigFileExistsAndResponseOK() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My <b>Notification</b> Title", "My Notification <div class=\"myclass\">Body</div>", "http://notification.url/target"));
    messagePublisher.send(new Message("mary", "token2", "ios", "My <b>Notification</b> Title",
            "\n" +
                    "\n" +
                    "My Notification \n" +
                    "\n" +
                    "<div class=\"myclass\">Body</div>",
            "http://notification.url/target"));

    // Then
    verify(httpClient, times(2)).execute(reqArgs.capture());

    List<HttpPost> httpUriRequests = reqArgs.getAllValues();
    assertNotNull(httpUriRequests);

    HttpPost httpUriRequest = httpUriRequests.get(0);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject data = message.getJSONObject("data");
    assertEquals("My <b>Notification</b> Title", data.getString("title"));
    assertEquals("My Notification <div class=\"myclass\">Body</div>", data.getString("body"));
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token1", message.getString("token"));
    assertFalse(message.has("android"));
    assertFalse(message.has("ios"));

    httpUriRequest = httpUriRequests.get(1);
    assertNotNull(httpUriRequest);
    body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    jsonMessage = new JSONObject(body.replaceAll("\\n", "\\\\n"));
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("notification");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification \n\nBody", notification.getString("body"));
    data = message.getJSONObject("data");
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token2", message.getString("token"));
    assertFalse(message.has("android"));
    assertTrue(message.has("apns"));
    JSONObject apns = message.getJSONObject("apns");
    assertTrue(apns.has("payload"));
    JSONObject payload = apns.getJSONObject("payload");
    assertTrue(payload.has("aps"));
    JSONObject aps = payload.getJSONObject("aps");
    assertTrue(aps.has("badge"));
    assertEquals(5, aps.getInt("badge"));
  }

  @Test
  public void shouldSendSanitizedHTMLMessageWithInlineImage() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My <b>Notification</b> Title", "My Notification <div class=\"myclass\">Text</div> <img data-plugin-name='insertImage' src=\"http://fake.com/image.png\"/> Text", "http://notification.url/target"));
    messagePublisher.send(new Message("mary", "token2", "ios", "My <b>Notification</b> Title", "My Notification <div class=\"myclass\">Text</div> <img data-plugin-name='insertImage' src=\"http://fake.com/image.png\"/> Text", "http://notification.url/target"));

    // Then
    verify(httpClient, times(2)).execute(reqArgs.capture());

    List<HttpPost> httpUriRequests = reqArgs.getAllValues();
    assertNotNull(httpUriRequests);

    HttpPost httpUriRequest = httpUriRequests.get(0);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject data = message.getJSONObject("data");
    assertEquals("My <b>Notification</b> Title", data.getString("title"));
    assertEquals("My Notification <div class=\"myclass\">Text</div> <i> [inline image] </i> Text", data.getString("body"));
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token1", message.getString("token"));
    assertFalse(message.has("android"));
    assertFalse(message.has("ios"));

    httpUriRequest = httpUriRequests.get(1);
    assertNotNull(httpUriRequest);
    body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("notification");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Text  [inline image]  Text", notification.getString("body"));
    data = message.getJSONObject("data");
    assertEquals("http://notification.url/target", data.getString("url"));
    assertEquals("token2", message.getString("token"));
    assertFalse(message.has("android"));
    assertTrue(message.has("apns"));
    JSONObject apns = message.getJSONObject("apns");
    assertTrue(apns.has("payload"));
    JSONObject payload = apns.getJSONObject("payload");
    assertTrue(payload.has("aps"));
    JSONObject aps = payload.getJSONObject("aps");
    assertTrue(aps.has("badge"));
    assertEquals(5, aps.getInt("badge"));
  }


  @Test
  public void shouldSendMessageWhenConfigFileExistsAndResponseNOK() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_UNAUTHORIZED, "Not Authorized"));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    try {
      // When
      messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body", ""));
      fail("An exception must be thrown when FCM returns an error");
    } catch(Exception e) {
      // Then
      verify(httpClient, times(1)).execute(reqArgs.capture());
      assertEquals("Error sending Push Notification, response is 401 - Not Authorized", e.getMessage());
    }
  }

  @Test
  public void shouldSendMessageWithTTLWhenConfigFileExistsAndResponseOK() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = buildInitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body", ""));
    messagePublisher.send(new Message("mary", "token2", "ios", "My Notification Title", "My Notification Body", ""));

    // Then
    verify(httpClient, times(2)).execute(reqArgs.capture());

    List<HttpPost> httpUriRequests = reqArgs.getAllValues();
    assertNotNull(httpUriRequests);
    HttpPost httpUriRequest = httpUriRequests.get(0);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("data");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Body", notification.getString("body"));
    assertEquals("token1", message.getString("token"));
    JSONObject android = message.getJSONObject("android");
    assertEquals("60s", android.getString("ttl"));
    assertFalse(message.has("ios"));

    httpUriRequest = httpUriRequests.get(1);
    assertNotNull(httpUriRequest);
    body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    message = jsonMessage.getJSONObject("message");
    notification = message.getJSONObject("notification");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Body", notification.getString("body"));
    assertEquals("token2", message.getString("token"));
    JSONObject ios = message.getJSONObject("apns");
    assertEquals("60s", android.getString("ttl"));
    assertFalse(message.has("ios"));
  }

  @Test
  public void shouldNotThrowInvalidTokenExceptionWhenResponseNotInvalidToken() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_BAD_REQUEST, ""));
    String invalidTokenResponse = "{\n" +
            "  \"error\": {\n" +
            "    \"code\": 400,\n" +
            "    \"message\": \"Request contains an invalid argument.\",\n" +
            "    \"status\": \"QUOTA_EXCEEDED\"\n" +
            "  }\n" +
            "}";
    HttpEntity httpEntity = new ByteArrayEntity(invalidTokenResponse.getBytes());
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = buildInitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    // When
    try {
      messagePublisher.send(new Message("", "", "", "", "", ""));
    } catch (InvalidTokenException e) {
      // Then
      fail("Should return an Exception, not an InvalidTokenException");
    } catch (Exception e) {
      // Then should return an Exception
    }
  }

  @Test(expected = InvalidTokenException.class)
  public void shouldThrowInvalidTokenExceptionWhenResponseUnregistered() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_BAD_REQUEST, ""));
    String invalidTokenResponse = "{\n" +
            "  \"error\": {\n" +
            "    \"code\": 400,\n" +
            "    \"message\": \"Request contains an invalid argument.\",\n" +
            "    \"status\": \"UNREGISTERED\"\n" +
            "  }\n" +
            "}";
    HttpEntity httpEntity = new ByteArrayEntity(invalidTokenResponse.getBytes());
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = buildInitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then should return an InvalidTokenException
  }


  @Test(expected = InvalidTokenException.class)
  public void shouldThrowInvalidTokenExceptionWhenResponseInvalidToken() throws Exception {
    // Given
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_BAD_REQUEST, ""));
    String invalidTokenResponse = "{\n" +
            "  \"error\": {\n" +
            "    \"code\": 400,\n" +
            "    \"message\": \"Request contains an invalid argument.\",\n" +
            "    \"status\": \"INVALID_ARGUMENT\",\n" +
            "    \"details\": [\n" +
            "      {\n" +
            "        \"@type\": \"type.googleapis.com/google.firebase.fcm.v1.FcmError\",\n" +
            "        \"errorCode\": \"INVALID_ARGUMENT\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"@type\": \"type.googleapis.com/google.rpc.BadRequest\",\n" +
            "        \"fieldViolations\": [\n" +
            "          {\n" +
            "            \"field\": \"message.token\",\n" +
            "            \"description\": \"Invalid registration token\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    HttpEntity httpEntity = new ByteArrayEntity(invalidTokenResponse.getBytes());
    when(httpResponse.getEntity()).thenReturn(httpEntity);
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = buildInitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, resourceBundleService, webNotificationService, httpClient) {
      @Override
      protected PrivateKey getPrivateKeyFromPkcs8(String privateKeyPem) throws IOException {
        return mock(PrivateKey.class);
      }
      @Override
      protected String getAccessToken() throws IOException {
        return "fakeAccessToken";
      }
    };

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then should return an InvalidTokenException
  }

  private InitParams buildInitParams() {
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    ValueParam messageExpirationTimeParam = new ValueParam();
    messageExpirationTimeParam.setName("messageExpirationTime");
    messageExpirationTimeParam.setValue("60");
    initParams.addParameter(messageExpirationTimeParam);
    return initParams;
  }
}
