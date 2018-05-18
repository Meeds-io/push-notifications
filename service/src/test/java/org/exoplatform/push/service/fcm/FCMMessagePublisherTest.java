package org.exoplatform.push.service.fcm;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.domain.Message;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FCMMessagePublisherTest {

  @Test
  public void shouldNotSendMessageWhenInitParamsAreNull() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(null, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldNotSendMessageWhenNoConfigFilePathParam() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    InitParams initParams = new InitParams();
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldNotSendMessageWhenNoConfigFile() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue("fake.json");
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldSendMessageWhenConfigFileExistsAndResponseOK() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, httpClient) {
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
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("data");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Body", notification.getString("body"));
    assertEquals("http://notification.url/target", notification.getString("url"));
    assertEquals("token1", message.getString("token"));
    assertFalse(message.has("android"));
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
    assertEquals("http://notification.url/target", notification.getString("url"));
    assertEquals("token2", message.getString("token"));
    assertFalse(message.has("android"));
    assertFalse(message.has("apns"));
  }

  @Test
  public void shouldSendMessageWhenConfigFileExistsAndResponseNOK() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_UNAUTHORIZED, "Not Authorized"));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, httpClient) {
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
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
    when(httpResponse.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("", 1, 2), HttpStatus.SC_OK, ""));
    when(httpClient.execute(any())).thenReturn(httpResponse);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serviceAccountFilePath");
    serverKeyParam.setValue(this.getClass().getResource("/fcm-test.json").getPath());
    initParams.addParameter(serverKeyParam);
    ValueParam messageExpirationTimeParam = new ValueParam();
    messageExpirationTimeParam.setName("messageExpirationTime");
    messageExpirationTimeParam.setValue("60");
    initParams.addParameter(messageExpirationTimeParam);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(initParams, httpClient) {
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
}