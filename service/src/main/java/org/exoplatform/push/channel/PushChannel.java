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

  public final static String LOG_SERVICE_NAME = "notifications";
  public final static String LOG_OPERATION_NAME = "send-push-notification";

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
              Message message = new Message(userId, device.getToken(), device.getType(), notificationTitle, messageInfo.getBody());
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
