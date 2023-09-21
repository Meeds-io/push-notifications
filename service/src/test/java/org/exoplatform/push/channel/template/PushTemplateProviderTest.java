/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.push.channel.template;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.notification.plugin.ActivityCommentPlugin;
import org.exoplatform.social.notification.plugin.ActivityMentionPlugin;
import org.exoplatform.social.notification.plugin.ActivityReplyToCommentPlugin;
import org.exoplatform.social.notification.plugin.LikeCommentPlugin;
import org.exoplatform.social.notification.plugin.LikePlugin;
import org.exoplatform.social.notification.plugin.NewUserPlugin;
import org.exoplatform.social.notification.plugin.PostActivityPlugin;
import org.exoplatform.social.notification.plugin.PostActivitySpaceStreamPlugin;
import org.exoplatform.social.notification.plugin.RelationshipReceivedRequestPlugin;
import org.exoplatform.social.notification.plugin.RequestJoinSpacePlugin;
import org.exoplatform.social.notification.plugin.SpaceInvitationPlugin;

public class PushTemplateProviderTest {

  @Test
  public void testGetTemplateBuilder() {
    PushTemplateProvider pushTemplateProvider = new PushTemplateProvider(new InitParams());
    assertNotNull(pushTemplateProvider.getTemplateBuilder());
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(ActivityCommentPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(ActivityReplyToCommentPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(ActivityMentionPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(LikePlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(LikeCommentPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(NewUserPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(PostActivityPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(PostActivitySpaceStreamPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(RelationshipReceivedRequestPlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(RequestJoinSpacePlugin.ID)));
    assertNotNull(pushTemplateProvider.getTemplateBuilder().get(PluginKey.key(SpaceInvitationPlugin.ID)));
  }

}
