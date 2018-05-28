package org.exoplatform.push.channel;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.domain.Message;
import org.exoplatform.push.exception.InvalidTokenException;
import org.exoplatform.push.service.DeviceService;
import org.exoplatform.push.service.MessagePublisher;
import org.exoplatform.push.service.fcm.FCMLegacyAPIMessagePublisher;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class PushChannelTest {

  @Test
  public void shouldSendNoNotifWhenDevicesNull() throws Exception {
    // Given
    MessagePublisher messagePublisher = mock(FCMLegacyAPIMessagePublisher.class);
    DeviceService deviceService = mock(DeviceService.class);
    NotificationContext ctx = mock(NotificationContext.class);
    NotificationInfo notificationInfo = mock(NotificationInfo.class);
    when(notificationInfo.getKey()).thenReturn(new PluginKey("pluginId"));
    when(ctx.getNotificationInfo()).thenReturn(notificationInfo);
    when(deviceService.getDevicesByUser(anyString())).thenReturn(null);

    PushChannel pushChannel = new PushChannel(messagePublisher, deviceService);

    // When
    pushChannel.dispatch(ctx, "john");

    // Then
    verify(messagePublisher, never()).send(any());
  }

  @Test
  public void shouldSendNoNotifWhenNoDevice() throws Exception {
    // Given
    MessagePublisher messagePublisher = mock(FCMLegacyAPIMessagePublisher.class);
    DeviceService deviceService = mock(DeviceService.class);
    NotificationContext ctx = mock(NotificationContext.class);
    NotificationInfo notificationInfo = mock(NotificationInfo.class);
    when(notificationInfo.getKey()).thenReturn(new PluginKey("pluginId"));
    when(ctx.getNotificationInfo()).thenReturn(notificationInfo);
    when(deviceService.getDevicesByUser(anyString())).thenReturn(new ArrayList<>());

    PushChannel pushChannel = new PushChannel(messagePublisher, deviceService);

    // When
    pushChannel.dispatch(ctx, "john");

    // Then
    verify(messagePublisher, never()).send(any());
  }

  @Test
  public void shouldSendNotifWhenDevicesExist() throws Exception {
    // Given
    MessagePublisher messagePublisher = mock(FCMLegacyAPIMessagePublisher.class);
    DeviceService deviceService = mock(DeviceService.class);
    NotificationContext ctx = mock(NotificationContext.class);
    NotificationInfo notificationInfo = mock(NotificationInfo.class);
    when(notificationInfo.getKey()).thenReturn(new PluginKey("pluginId"));
    when(ctx.getNotificationInfo()).thenReturn(notificationInfo);
    when(deviceService.getDevicesByUser(anyString())).thenReturn(Arrays.asList(new Device(), new Device()));

    PushChannel pushChannel = new PushChannel(messagePublisher, deviceService);

    ArgumentCaptor<Message> messageArgs = ArgumentCaptor.forClass(Message.class);

    // When
    pushChannel.dispatch(ctx, "john");

    // Then
    verify(messagePublisher, times(2)).send(messageArgs.capture());
    List<Message> messages = messageArgs.getAllValues();
    assertNotNull(messages);
    assertEquals(2, messages.size());
  }

  @Test
  public void shouldDeleteDeviceWhenTokenIsInvalid() throws Exception {
    // Given
    MessagePublisher messagePublisher = mock(FCMLegacyAPIMessagePublisher.class);
    DeviceService deviceService = mock(DeviceService.class);
    NotificationContext ctx = mock(NotificationContext.class);
    NotificationInfo notificationInfo = mock(NotificationInfo.class);
    doThrow(new InvalidTokenException()).when(messagePublisher).send(any(Message.class));
    when(notificationInfo.getKey()).thenReturn(new PluginKey("pluginId"));
    when(ctx.getNotificationInfo()).thenReturn(notificationInfo);
    Device device = new Device();
    device.setToken("token1");
    when(deviceService.getDevicesByUser(anyString())).thenReturn(Arrays.asList(device));

    PushChannel pushChannel = new PushChannel(messagePublisher, deviceService);

    // When
    pushChannel.dispatch(ctx, "john");

    // Then
    ArgumentCaptor<Message> messageArgs = ArgumentCaptor.forClass(Message.class);
    verify(messagePublisher, times(1)).send(messageArgs.capture());
    List<Message> messages = messageArgs.getAllValues();
    assertNotNull(messages);
    assertEquals(1, messages.size());

    ArgumentCaptor<String> tokenArgs = ArgumentCaptor.forClass(String.class);
    verify(deviceService, times(1)).getDeviceByToken(tokenArgs.capture());
    String token = tokenArgs.getValue();
    assertEquals("token1", token);
  }
}