/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.push.channel.template;

import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.notification.channel.template.WebTemplateProvider;
import org.exoplatform.social.notification.plugin.*;
import org.exoplatform.wcm.notification.plugin.ShareFileToSpacePlugin;
import org.exoplatform.wcm.notification.plugin.ShareFileToUserPlugin;

/**
 * Templates for Push Notifications.
 * It extends WebTemplateProvider since we want the same information for wen notifications.
 */
@TemplateConfigs (
  templates = {
    @TemplateConfig(pluginId = ActivityCommentPlugin.ID, template = "war:/push-notifications/templates/ActivityCommentPlugin.gtmpl"),
    @TemplateConfig(pluginId = ActivityMentionPlugin.ID, template = "war:/push-notifications/templates/ActivityMentionPlugin.gtmpl"),
    @TemplateConfig(pluginId = LikePlugin.ID, template = "war:/push-notifications/templates/LikePlugin.gtmpl"),
    @TemplateConfig(pluginId = NewUserPlugin.ID, template = "war:/push-notifications/templates/NewUserPlugin.gtmpl"),
    @TemplateConfig(pluginId = PostActivityPlugin.ID, template = "war:/push-notifications/templates/PostActivityPlugin.gtmpl"),
    @TemplateConfig(pluginId = PostActivitySpaceStreamPlugin.ID, template = "war:/push-notifications/templates/PostActivitySpaceStreamPlugin.gtmpl"),
    @TemplateConfig(pluginId = RelationshipReceivedRequestPlugin.ID, template = "war:/push-notifications/templates/RelationshipReceivedRequestPlugin.gtmpl"),
    @TemplateConfig(pluginId = RequestJoinSpacePlugin.ID, template = "war:/push-notifications/templates/RequestJoinSpacePlugin.gtmpl"),
    @TemplateConfig(pluginId = SpaceInvitationPlugin.ID, template = "war:/push-notifications/templates/SpaceInvitationPlugin.gtmpl"),
    @TemplateConfig(pluginId = ShareFileToUserPlugin.ID, template = "war:/push-notifications/templates/ShareDocumentToUser.gtmpl"),
    @TemplateConfig(pluginId = ShareFileToSpacePlugin.ID, template = "war:/push-notifications/templates/ShareDocumentToSpace.gtmpl")

  }
)
public class PushTemplateProvider extends WebTemplateProvider {

  public PushTemplateProvider(InitParams initParams) {
    super(initParams);
  }
}
