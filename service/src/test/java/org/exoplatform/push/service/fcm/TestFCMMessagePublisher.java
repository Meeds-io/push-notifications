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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TestFCMMessagePublisher {

  @Test
  public void shouldNotSendMessageWhenInitParamsAreNull() throws Exception {
    // Given
    CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
    FCMMessagePublisher messagePublisher = new FCMMessagePublisher(null, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", ""));

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
    messagePublisher.send(new Message("", "", "", "", ""));

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
    messagePublisher.send(new Message("", "", "", "", ""));

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
    messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body"));

    // Then
    verify(httpClient, times(1)).execute(reqArgs.capture());
    HttpPost httpUriRequest = reqArgs.getValue();
    assertNotNull(httpUriRequest);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals(false, jsonMessage.getBoolean("validate_only"));
    JSONObject message = jsonMessage.getJSONObject("message");
    JSONObject notification = message.getJSONObject("notification");
    assertEquals("My Notification Title", notification.getString("title"));
    assertEquals("My Notification Body", notification.getString("body"));
    assertEquals("token1", message.getString("token"));
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
      messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body"));
      fail("An exception must be thrown when FCM returns an error");
    } catch(Exception e) {
      // Then
      verify(httpClient, times(1)).execute(reqArgs.capture());
      assertEquals("Error sending Push Notification, response is 401 - Not Authorized", e.getMessage());
    }
  }
}