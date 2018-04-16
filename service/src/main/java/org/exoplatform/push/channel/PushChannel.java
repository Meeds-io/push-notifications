package org.exoplatform.push.channel;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.AbstractChannel;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.domain.Message;
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

  public String notificationTitle;

  private final ChannelKey key = ChannelKey.key(ID);

  private final Map<PluginKey, String> templateFilePaths = new HashMap<PluginKey, String>();

  private final Map<PluginKey, AbstractTemplateBuilder> templateBuilders = new HashMap<PluginKey, AbstractTemplateBuilder>();

  private MessagePublisher messagePublisher;

  private DeviceService deviceService;

  public PushChannel(MessagePublisher messagePublisher, DeviceService deviceService) {
    this.messagePublisher = messagePublisher;
    this.deviceService = deviceService;

    this.notificationTitle = System.getProperty("exo.notifications.portalname");
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
    String templateFilePath = this.templateFilePaths.get(pluginId);
    LOG.info("Push::{ userId:" + userId + ", pluginId: " + pluginId + ", templateFilePath: " + templateFilePath + "}");

    List<Device> devices = deviceService.getDevicesByUser(userId);

    if(devices != null) {
      devices.forEach(device -> {
        try {
          AbstractTemplateBuilder builder = getTemplateBuilder(ctx.getNotificationInfo().getKey());
          if (builder != null) {
            MessageInfo messageInfo = builder.buildMessage(ctx);
            if (messageInfo != null) {
              LOG.info("Sending push notification to user {} (token={})",
                      userId, StringUtil.mask(device.getToken(), 4));
              Message message = new Message(userId, device.getToken(), device.getType(), notificationTitle, messageInfo.getBody());
              messagePublisher.send(message);
            }
          }
        } catch (Exception e) {
          LOG.error("Cannot send push notification to user " + userId, e);
        }
      });
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
