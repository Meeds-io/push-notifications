/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.social.notification.channel.template.WebTemplateProvider;
import org.exoplatform.social.notification.plugin.*;

/**
 * Templates for Push Notifications.
 * It extends WebTemplateProvider since we want the same information for web notifications, but it
 * re-calculate target URL in order to pass it as a standalone data to the push notifications manager.
 * The target URL is passed in the field "subject" of the MessageInfo object since there is no field to pass custom data.
 * TODO Improve MessageInfo to allow to pass custom data
 */
@TemplateConfigs (
  templates = {
    @TemplateConfig(pluginId = ActivityCommentPlugin.ID, template = "war:/push-notifications/templates/ActivityCommentPlugin.gtmpl"),
    @TemplateConfig(pluginId = ActivityReplyToCommentPlugin.ID, template = "war:/push-notifications/templates/ActivityReplyToCommentPlugin.gtmpl"),
    @TemplateConfig(pluginId = ActivityMentionPlugin.ID, template = "war:/push-notifications/templates/ActivityMentionPlugin.gtmpl"),
    @TemplateConfig(pluginId = LikePlugin.ID, template = "war:/push-notifications/templates/LikePlugin.gtmpl"),
    @TemplateConfig(pluginId = LikeCommentPlugin.ID, template = "war:/push-notifications/templates/LikeCommentPlugin.gtmpl"),
    @TemplateConfig(pluginId = NewUserPlugin.ID, template = "war:/push-notifications/templates/NewUserPlugin.gtmpl"),
    @TemplateConfig(pluginId = PostActivityPlugin.ID, template = "war:/push-notifications/templates/PostActivityPlugin.gtmpl"),
    @TemplateConfig(pluginId = PostActivitySpaceStreamPlugin.ID, template = "war:/push-notifications/templates/PostActivitySpaceStreamPlugin.gtmpl"),
    @TemplateConfig(pluginId = RelationshipReceivedRequestPlugin.ID, template = "war:/push-notifications/templates/RelationshipReceivedRequestPlugin.gtmpl"),
    @TemplateConfig(pluginId = RequestJoinSpacePlugin.ID, template = "war:/push-notifications/templates/RequestJoinSpacePlugin.gtmpl"),
    @TemplateConfig(pluginId = SpaceInvitationPlugin.ID, template = "war:/push-notifications/templates/SpaceInvitationPlugin.gtmpl"),

  }
)
public class PushTemplateProvider extends WebTemplateProvider {

  private final Map<PluginKey, AbstractTemplateBuilder> webTemplateBuilders = new HashMap<>();

  /** Defines the template builder for ActivityCommentPlugin*/
  private AbstractTemplateBuilder comment = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(ActivityCommentPlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      boolean notHighLightComment = Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.NOT_HIGHLIGHT_COMMENT_PORPERTY.getKey()));
      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      ExoSocialActivity comment;
      if (activity.isComment()) {
        comment = activity;
        activity = Utils.getActivityManager().getParentActivity(comment);
        notification.with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId());
        notification.with(SocialNotificationUtils.COMMENT_ID.getKey(), comment.getId());
      } else {
        comment = Utils.getActivityManager().getActivity(notification.getValueOwnerParameter(SocialNotificationUtils.COMMENT_ID.getKey()));
      }

      String url = CommonsUtils.getCurrentDomain() + LinkProvider.getSingleActivityUrl(notHighLightComment ? activity.getId() : activity.getId() + "#comment-" + comment.getId());

      return messageInfo.subject(url).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for ActivityReplyToCommentPlugin*/
  private AbstractTemplateBuilder replyToComment = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(ActivityReplyToCommentPlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      boolean notHighLightComment = Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.NOT_HIGHLIGHT_COMMENT_PORPERTY.getKey()));
      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String replyToCommentId = notification.getValueOwnerParameter(SocialNotificationUtils.COMMENT_REPLY_ID.getKey());

      String url = CommonsUtils.getCurrentDomain() + LinkProvider.getSingleActivityUrl(notHighLightComment ? activityId : activityId + "#comment-" + replyToCommentId);

      return messageInfo.subject(url).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for ActivityMentionPlugin*/
  private AbstractTemplateBuilder mention = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(ActivityMentionPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getActivityUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for LikePlugin*/
  private AbstractTemplateBuilder like = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(LikePlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getActivityUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for LikeCommentPlugin*/
  private AbstractTemplateBuilder likeComment = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(LikeCommentPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getActivityUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for NewUserPlugin*/
  private AbstractTemplateBuilder newUser = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(NewUserPlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      String remoteId = notification.getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey());
      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
      String url = CommonsUtils.getCurrentDomain() + LinkProvider.getUserProfileUri(identity.getRemoteId());

      return messageInfo.subject(url).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for PostActivityPlugin*/
  private AbstractTemplateBuilder postActivity = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(PostActivityPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getActivityUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };


  /** Defines the template builder for PostActivitySpaceStreamPlugin*/
  private AbstractTemplateBuilder postActivitySpace = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(PostActivitySpaceStreamPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getActivityUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };


  /** Defines the template builder for RelationshipReceivedRequestPlugin*/
  private AbstractTemplateBuilder relationshipReceived = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(RelationshipReceivedRequestPlugin.ID)).buildMessage(ctx);

      NotificationInfo notification = ctx.getNotificationInfo();
      String sender = notification.getValueOwnerParameter("sender");
      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, sender, true);

      String url = CommonsUtils.getCurrentDomain() + LinkProvider.getUserProfileUri(identity.getRemoteId());

      return messageInfo.subject(url).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }

  };


  /** Defines the template builder for RequestJoinSpacePlugin*/
  private AbstractTemplateBuilder requestJoinSpace = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(RequestJoinSpacePlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getSpaceUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for SpaceInvitationPlugin*/
  private AbstractTemplateBuilder spaceInvitation = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      MessageInfo messageInfo = webTemplateBuilders.get(new PluginKey(SpaceInvitationPlugin.ID)).buildMessage(ctx);

      return messageInfo.subject(getSpaceUrl(ctx)).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };


  public PushTemplateProvider(InitParams initParams) {
    super(initParams);
    this.webTemplateBuilders.putAll(this.templateBuilders);
    this.templateBuilders.put(PluginKey.key(ActivityCommentPlugin.ID), comment);
    this.templateBuilders.put(PluginKey.key(ActivityReplyToCommentPlugin.ID), replyToComment);
    this.templateBuilders.put(PluginKey.key(ActivityMentionPlugin.ID), mention);
    this.templateBuilders.put(PluginKey.key(LikePlugin.ID), like);
    this.templateBuilders.put(PluginKey.key(LikeCommentPlugin.ID), likeComment);
    this.templateBuilders.put(PluginKey.key(NewUserPlugin.ID), newUser);
    this.templateBuilders.put(PluginKey.key(PostActivityPlugin.ID), postActivity);
    this.templateBuilders.put(PluginKey.key(PostActivitySpaceStreamPlugin.ID), postActivitySpace);
    this.templateBuilders.put(PluginKey.key(RelationshipReceivedRequestPlugin.ID), relationshipReceived);
    this.templateBuilders.put(PluginKey.key(RequestJoinSpacePlugin.ID), requestJoinSpace);
    this.templateBuilders.put(PluginKey.key(SpaceInvitationPlugin.ID), spaceInvitation);
  }

  /**
   * Retrieve the direct URL of the activity referenced in the notification context
   * @param ctx The notification context, which must contain the parameter "activityId"
   * @return The activity URL
   */
  private String getActivityUrl(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
    ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);

    String url;
    if (activity.isComment()) {
      ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
      activityId = parentActivity.getId();
      url = CommonsUtils.getCurrentDomain() + LinkProvider.getSingleActivityUrl(activityId + "#comment-" + activity.getId());
    } else {
      url = CommonsUtils.getCurrentDomain() + LinkProvider.getSingleActivityUrl(activityId);
    }

    return url;
  }

  /**
   * Retrieve the direct URL of the space referenced in the notification context
   * @param ctx The notification context, which must contain the parameter "spaceId"
   * @return The space URL
   */
  private String getSpaceUrl(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
    Space space = Utils.getSpaceService().getSpaceById(spaceId);

    return CommonsUtils.getCurrentDomain() + LinkProvider.getActivityUriForSpace(space.getPrettyName(), space.getGroupId().replace("/spaces/", ""));
  }
}
