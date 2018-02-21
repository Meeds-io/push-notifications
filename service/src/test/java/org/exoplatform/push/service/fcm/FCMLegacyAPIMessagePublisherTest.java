package org.exoplatform.push.service.fcm;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.push.domain.Message;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class FCMLegacyAPIMessagePublisherTest {

  @Test
  public void shouldNotSendMessageWhenInitParamsAreNull() throws Exception {
    // Given
    HttpClient httpClient = mock(HttpClient.class);
    FCMLegacyAPIMessagePublisher messagePublisher = new FCMLegacyAPIMessagePublisher(null, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldNotSendMessageWhenNoServerKey() throws Exception {
    // Given
    HttpClient httpClient = mock(HttpClient.class);
    InitParams initParams = new InitParams();
    FCMLegacyAPIMessagePublisher messagePublisher = new FCMLegacyAPIMessagePublisher(initParams, httpClient);

    // When
    messagePublisher.send(new Message("", "", "", "", ""));

    // Then
    verify(httpClient, never()).execute(any());
  }

  @Test
  public void shouldSendMessageWhenServerKeyExists() throws Exception {
    // Given
    HttpClient httpClient = mock(HttpClient.class);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serverKey");
    serverKeyParam.setValue("fakeServerKey");
    initParams.addParameter(serverKeyParam);
    FCMLegacyAPIMessagePublisher messagePublisher = new FCMLegacyAPIMessagePublisher(initParams, httpClient);

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body"));

    // Then
    verify(httpClient, times(1)).execute(reqArgs.capture());
    HttpPost httpUriRequest = reqArgs.getValue();
    assertNotNull(httpUriRequest);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals("token1", jsonMessage.getString("to"));
    assertEquals("My Notification Title", jsonMessage.getJSONObject("notification").getString("title"));
    assertEquals("My Notification Body", jsonMessage.getJSONObject("notification").getString("body"));
    assertFalse(jsonMessage.has("time_to_live"));
  }

  @Test
  public void shouldSendMessageWithTTLWhenServerKeyExists() throws Exception {
    // Given
    HttpClient httpClient = mock(HttpClient.class);
    InitParams initParams = new InitParams();
    ValueParam serverKeyParam = new ValueParam();
    serverKeyParam.setName("serverKey");
    serverKeyParam.setValue("fakeServerKey");
    initParams.addParameter(serverKeyParam);
    ValueParam messageExpirationTimeParam = new ValueParam();
    messageExpirationTimeParam.setName("messageExpirationTime");
    messageExpirationTimeParam.setValue("30");
    initParams.addParameter(messageExpirationTimeParam);
    FCMLegacyAPIMessagePublisher messagePublisher = new FCMLegacyAPIMessagePublisher(initParams, httpClient);

    ArgumentCaptor<HttpPost> reqArgs = ArgumentCaptor.forClass(HttpPost.class);

    // When
    messagePublisher.send(new Message("john", "token1", "android", "My Notification Title", "My Notification Body"));

    // Then
    verify(httpClient, times(1)).execute(reqArgs.capture());
    HttpPost httpUriRequest = reqArgs.getValue();
    assertNotNull(httpUriRequest);
    String body = IOUtils.toString(httpUriRequest.getEntity().getContent(), "UTF-8");
    JSONObject jsonMessage = new JSONObject(body);
    assertEquals("token1", jsonMessage.getString("to"));
    assertEquals("My Notification Title", jsonMessage.getJSONObject("notification").getString("title"));
    assertEquals("My Notification Body", jsonMessage.getJSONObject("notification").getString("body"));
    assertEquals("30", jsonMessage.getString("time_to_live"));
  }
}