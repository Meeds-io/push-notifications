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
package org.exoplatform.push.upgrade;

import static org.exoplatform.commons.notification.impl.AbstractService.EXO_IS_ACTIVE;

import java.util.List;

import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.notification.channel.WebChannel;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.push.channel.PushChannel;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PushNotificationSettingsUpgradePlugin extends UpgradeProductPlugin {

  private static final Log     LOG = ExoLogger.getLogger(PushNotificationSettingsUpgradePlugin.class);

  private SettingService       settingService;

  private UserSettingService   userSettingService;

  private PluginSettingService pluginSettingService;

  private EntityManagerService entityManagerService;

  public PushNotificationSettingsUpgradePlugin(SettingService settingService,
                                               UserSettingService userSettingService,
                                               PluginSettingService pluginSettingService,
                                               EntityManagerService entityManagerService,
                                               InitParams initParams) {
    super(settingService, initParams);
    this.settingService = settingService;
    this.userSettingService = userSettingService;
    this.pluginSettingService = pluginSettingService;
    this.entityManagerService = entityManagerService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    int pageSize = 20;
    int current = 0;
    ExoContainer currentContainer = ExoContainerContext.getCurrentContainer();
    try {
      LOG.info("=== Start initialisation of Push Notifications settings");

      LOG.info("  Activate Push Notifications channel for all notification plugins");
      pluginSettingService.getAllPlugins()
                          .forEach(pluginInfo -> {
                            pluginSettingService.saveActivePlugin(PushChannel.ID,
                                    pluginInfo.getType(),
                                    true);
                            // Hack to update the PluginInfo objects loaded in memory in PluginSettingService.
                            // It relies on the fact that the method getGroupPlugins return references to the loaded
                            // PluginInfo objects, so we can update them.
                            // To fix it correctly, PluginSettingService should be updated to update these PluginInfo
                            // objects loaded in memory when saveActivePlugin is called.
                            pluginSettingService.getGroupPlugins().forEach(groupProvider -> {
                              groupProvider.getPluginInfos().stream()
                                      .filter(groupPluginInfo -> groupPluginInfo.getType().equals(pluginInfo.getType()))
                                      .findFirst()
                                      .ifPresent(matchedPluginInfo -> matchedPluginInfo.setChannelActive(PushChannel.ID));
                            });
                          });

      LOG.info("  Starting activating Push Notifications for users");

      List<Context> usersContexts;

      entityManagerService.startRequest(currentContainer);
      long startTime = System.currentTimeMillis();
      do {
        LOG.info("  Progression of users Push Notifications settings initialisation : {} users", current);

        // Get all users who already update their notification settings
        usersContexts = settingService.getContextsByTypeAndScopeAndSettingName(Context.USER.getName(), Scope.APPLICATION.getName(),
                "NOTIFICATION", EXO_IS_ACTIVE, current, pageSize);

        if(usersContexts != null) {
          for (Context userContext : usersContexts) {
            try {
              entityManagerService.endRequest(currentContainer);
              entityManagerService.startRequest(currentContainer);
              String userName = userContext.getId();

              SettingValue<?> pushNotificationChannelSetting = settingService.get(userContext,
                      Scope.APPLICATION.id("NOTIFICATION"),
                      "exo:PUSH_CHANNELChannel");
              if (pushNotificationChannelSetting != null) {
                // update push notifications settings only if the push channel have never been
                // configured
                continue;
              }

              UserSetting userSetting = this.userSettingService.get(userName);
              if (userSetting.isChannelActive(WebChannel.ID)) {
                userSetting.setChannelActive(PushChannel.ID);
              } else {
                userSetting.removeChannelActive(PushChannel.ID);
              }
              List<String> plugins = userSetting.getPlugins(WebChannel.ID);
              userSetting.setChannelPlugins(PushChannel.ID, plugins);
              userSettingService.save(userSetting);
            } catch (Exception e) {
              LOG.error("  Error while activating Push Notifications for user " + userContext.getId(), e);
            }
          }
          current += usersContexts.size();
        }
      } while (usersContexts != null && !usersContexts.isEmpty());
      long endTime = System.currentTimeMillis();
      LOG.info("  Users Push Notifications settings initialised in " + (endTime - startTime) + " ms");
    } catch (Exception e) {
      LOG.error("Error while initialisation of users Push Notifications settings - Cause : " + e.getMessage(), e);
    } finally {
      entityManagerService.endRequest(currentContainer);
    }

    LOG.info("=== {} users with modified notifications settings have been found and processed successfully", current);
    LOG.info("=== End initialisation of Push Notifications settings");
  }
}
