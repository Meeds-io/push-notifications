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
package org.exoplatform.push.channel;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.domain.Message;
import org.exoplatform.push.exception.InvalidTokenException;
import org.exoplatform.push.service.DeviceService;
import org.exoplatform.push.service.MessagePublisher;
import org.exoplatform.push.util.StringUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushChannel extends AbstractChannel {

  private static final Log LOG = ExoLogger.getLogger(PushChannel.class);

  public final static String ID = "PUSH_CHANNEL";

  public final static String LOG_SERVICE_NAME = "notifications";
  public final static String LOG_OPERATION_NAME = "send-push-notification";

  private final ChannelKey key = ChannelKey.key(ID);

  private final Map<PluginKey, String> templateFilePaths = new HashMap<PluginKey, String>();

  private final Map<PluginKey, AbstractTemplateBuilder> templateBuilders = new HashMap<PluginKey, AbstractTemplateBuilder>();

  private MessagePublisher messagePublisher;

  private DeviceService deviceService;
  private BrandingService brandingService;

  public PushChannel(MessagePublisher messagePublisher, DeviceService deviceService, BrandingService brandingService) {
    this.messagePublisher = messagePublisher;
    this.deviceService = deviceService;
    this.brandingService = brandingService;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public ChannelKey getKey() {
    return key;
  }

  @Override
  public void dispatch(NotificationContext ctx, String userId) {
    NotificationInfo notificationInfo = ctx.getNotificationInfo();
    String pluginId = notificationInfo.getKey().getId();
    LOG.debug("Received push notification sending order for user {} and pluginId {}", userId, pluginId);

    List<Device> devices = deviceService.getDevicesByUser(userId);

    if(devices != null) {
      LOG.debug("Found {} registered devices for user {}", devices.size(), userId);
      devices.forEach(device -> {
        long startTimeSendingMessage = System.currentTimeMillis();
        try {
          AbstractTemplateBuilder builder = getTemplateBuilder(ctx.getNotificationInfo().getKey());
          if (builder != null) {
            MessageInfo messageInfo = builder.buildMessage(ctx);
            if (messageInfo != null) {
              String maskedToken = StringUtil.mask(device.getToken(), 4);
              LOG.info("Sending push notification to user {} (token={})",
                      userId, maskedToken);
              if(StringUtils.isBlank(messageInfo.getSubject())) {
                if(LOG.isDebugEnabled()) {
                  LOG.warn("No URL provided for notification type {}", messageInfo.getPluginId());
                }
              }
              Message message = new Message(userId, device.getToken(), device.getType(), brandingService.getCompanyName(), messageInfo.getBody(), messageInfo.getSubject());
              messagePublisher.send(message);
              long sendMessageExecutionTime = System.currentTimeMillis() - startTimeSendingMessage;
              LOG.info("service={} operation={} parameters=\"user:{},token:{},type:{},pluginId:{}\" status=ok duration_ms={}", 
                      LOG_SERVICE_NAME, LOG_OPERATION_NAME, userId, maskedToken, device.getType(), pluginId, sendMessageExecutionTime);
            }
          }
        } catch (Exception e) {
          long sendMessageExecutionTime = System.currentTimeMillis() - startTimeSendingMessage;
          LOG.error("Cannot send push notification to user " + userId, e);
          LOG.info("service={} operation={} parameters=\"user:{},token:{},type:{},pluginId:{}\" status=ko duration_ms={} error_msg=\"{}\"", 
                      LOG_SERVICE_NAME, LOG_OPERATION_NAME, userId, StringUtil.mask(device.getToken(), 4), device.getType(), pluginId, sendMessageExecutionTime, e.getMessage());

          if(e instanceof InvalidTokenException) {
            LOG.info("Removing device of user {} (token={}) since the token is invalid", userId, StringUtil.mask(device.getToken(), 4));
            deviceService.deleteDevice(deviceService.getDeviceByToken(device.getToken()));
          }
        }
      });
    } else {
      LOG.debug("No device registered for user {}", userId);
    }
  }

  @Override
  public void registerTemplateProvider(TemplateProvider templateProvider) {
    this.templateFilePaths.putAll(templateProvider.getTemplateFilePathConfigs());
    this.templateBuilders.putAll(templateProvider.getTemplateBuilder());
  }

  @Override
  public String getTemplateFilePath(PluginKey key) {
    return this.templateFilePaths.get(key);
  }

  @Override
  public boolean hasTemplateBuilder(PluginKey key) {
    AbstractTemplateBuilder builder = this.templateBuilders.get(key);
    return builder != null;
  }

  @Override
  protected AbstractTemplateBuilder getTemplateBuilderInChannel(PluginKey pluginKey) {
    return this.templateBuilders.get(pluginKey);
  }
}
