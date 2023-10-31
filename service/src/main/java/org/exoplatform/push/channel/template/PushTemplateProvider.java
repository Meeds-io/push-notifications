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
package org.exoplatform.push.channel.template;

import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
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
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;
import org.exoplatform.social.notification.plugin.SpaceInvitationPlugin;
import org.exoplatform.webui.utils.TimeConvertUtils;

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
    @TemplateConfig(pluginId = SpaceInvitationPlugin.ID, template = "war:/push-notifications/templates/SpaceInvitationPlugin.gtmpl")
  }
)
public class PushTemplateProvider extends TemplateProvider {
  private static final Log LOG = ExoLogger.getLogger(PushTemplateProvider.class);

  private static final String ACCEPT_INVITATION_TO_CONNECT = "social/intranet-notification/confirmInvitationToConnect";
  private static final String REFUSE_INVITATION_TO_CONNECT = "social/intranet-notification/ignoreInvitationToConnect";
  private static final String MESSAGE_JSON_FILE_NAME = "message.json";

  private static final String VALIDATE_SPACE_REQUEST = "social/intranet-notification/validateRequestToJoinSpace";
  private static final String REFUSE_SPACE_REQUEST = "social/intranet-notification/refuseRequestToJoinSpace";
  private static final String ACCEPT_SPACE_INVITATION = "social/intranet-notification/acceptInvitationToJoinSpace";
  private static final String REFUSE_SPACE_INVITATION = "social/intranet-notification/ignoreInvitationToJoinSpace";
  /** Defines the template builder for ActivityCommentPlugin*/
  private AbstractTemplateBuilder comment = new AbstractTemplateBuilder() {

    /**
     * This method get the unread comment notification for a user and adds the names
     * of the new commenter in notification. In addition, it places the comment id and
     * activity id parameters.
     */
    @Override
    public NotificationInfo getNotificationToStore(NotificationInfo notification) {
      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String parameterName = SocialNotificationUtils.POSTER.getKey();
      String parameterValue = notification.getValueOwnerParameter(parameterName);
      return SocialNotificationUtils.addUserToPreviousNotification(notification, parameterName, activityId, parameterValue);
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      boolean isPopupOverOnly = ctx.value(WebNotificationService.POPUP_OVER);

      String language = getLanguage(notification);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String commentId = notification.getValueOwnerParameter(SocialNotificationUtils.COMMENT_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      ExoSocialActivity commentActivity = null;
      if (StringUtils.isNotBlank(commentId)) {
        commentActivity = Utils.getActivityManager().getActivity(commentId);
      }
      if (activity == null) {
        LOG.debug("Activity with id '{}' was removed but the notification with id'{}' is remaining", activityId, notification.getId());
        return null;
      }
      if(activity.isComment()) {
        commentActivity = Utils.getActivityManager().getParentActivity(activity);
      }
      if (commentActivity == null) {
        LOG.debug("Comment of activity with id '{}' was removed but the notification with id'{}' is remaining", commentId, notification.getId());
        return null;
      }
      if (!Utils.getActivityManager().isNotificationEnabled(commentActivity, notification.getTo())) {
        return null;
      }
      String pluginId = notification.getKey().getId();

      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(getActivityTitle(activity), activity.getType()));
      templateContext.put("COMMENT", isPopupOverOnly ? cutStringByMaxLength(getActivityTitle(commentActivity), 30) : getActivityTitle(commentActivity));
      List<String> users = SocialNotificationUtils.mergeUsers(notification, SocialNotificationUtils.POSTER.getKey(), activity.getId(), notification.getValueOwnerParameter(SocialNotificationUtils.POSTER.getKey()));

      //
      int nbUsers = users.size();
      if (nbUsers > 0) {
        Identity lastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 1), true);
        Profile profile = lastIdentity.getProfile();
        templateContext.put("USER", Utils.addExternalFlag(lastIdentity));
        templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
        templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(lastIdentity.getRemoteId()));
        templateContext.put("NB_USERS", nbUsers);
        //
        if (nbUsers >= 2) {
          Identity beforeLastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 2), true);
          templateContext.put("LAST_USER",  Utils.addExternalFlag(beforeLastIdentity));
          if (nbUsers > 2) {
            templateContext.put("COUNT", nbUsers - 2);
          }
        }
      }
      //
      boolean notHighLightComment = Boolean.parseBoolean(notification.getValueOwnerParameter(NotificationMessageUtils.NOT_HIGHLIGHT_COMMENT_PORPERTY.getKey()));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(notHighLightComment ?  activity.getId() : activity.getId() + "#comment-" + commentActivity.getId()));

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    private String cutStringByMaxLength(String st, int maxLength) {
      if (st == null) return st;
      st = StringEscapeUtils.unescapeHtml(st);
      if (st.length() <= maxLength) return st;
      String noHtmlSt = st.replaceAll("\\<.*?\\>", "");
      if (noHtmlSt.length() <= maxLength) return noHtmlSt;
      return noHtmlSt.substring(0, maxLength) + "...";
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for ActivityReplyToCommentPlugin*/
  private AbstractTemplateBuilder replyToComment = new AbstractTemplateBuilder() {

    /**
     * This method get the unread comment notification for a user and adds the names
     * of the new commenter in notification. In addition, it places the comment id and
     * activity id parameters.
     */
    @Override
    public NotificationInfo getNotificationToStore(NotificationInfo notification) {
      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String parameterName = SocialNotificationUtils.POSTER.getKey();
      String parameterValue = notification.getValueOwnerParameter(parameterName);
      return SocialNotificationUtils.addUserToPreviousNotification(notification, parameterName, activityId, parameterValue);
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      boolean isPopupOverOnly = ctx.value(WebNotificationService.POPUP_OVER);

      String language = getLanguage(notification);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String commentId = notification.getValueOwnerParameter(SocialNotificationUtils.COMMENT_ID.getKey());
      String replyToCommentId = notification.getValueOwnerParameter(SocialNotificationUtils.COMMENT_REPLY_ID.getKey());

      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      ExoSocialActivity commentActivity = commentId == null ? null : Utils.getActivityManager().getActivity(commentId);
      ExoSocialActivity replyToCommentActivity = replyToCommentId == null ? null : Utils.getActivityManager().getActivity(replyToCommentId);

      if (commentActivity != null) {
        if(commentActivity.getParentCommentId() == null) {
          if (replyToCommentActivity == null) {
            throw new IllegalStateException("Reply to comment ID is missing in context");
          }
        } else {
          replyToCommentActivity = commentActivity;
          commentActivity = null;

          replyToCommentId = replyToCommentActivity.getId();
          commentId = replyToCommentActivity.getParentCommentId();
          activityId = replyToCommentActivity.getParentId();

          if (activity != null && activity.getId().equals(commentId)) {
            commentActivity = activity;
          }
          if (activity != null && !StringUtils.equals(activity.getId(), activityId)) {
            activity = null;
          }
        }
      }

      if(activity != null) {
        if (activity.isComment()) {
          if(activity.getParentCommentId() == null) {
            commentActivity = activity;
            activity = null;

            commentId = commentActivity.getId();
            activityId = commentActivity.getParentId();
          } else {
            replyToCommentActivity = activity;
            commentActivity = null;
            activity = null;

            replyToCommentId = replyToCommentActivity.getId();
            commentId = replyToCommentActivity.getParentCommentId();
            activityId = replyToCommentActivity.getParentId();
          }
        }
      }

      if (activity == null) {
        if (StringUtils.isBlank(activityId)) {
          throw new IllegalStateException("Cannot find Activity ID in context");
        }
        activity = Utils.getActivityManager().getActivity(activityId);
        if (activity == null) {
          LOG.warn("Cannot find Activity with id '{}', it will not be displayed in notifications", activityId);
          return null;
        }
      }
      if (commentActivity == null) {
        if (StringUtils.isBlank(commentId)) {
          throw new IllegalStateException("Cannot find Parent Comment ID in context");
        }
        commentActivity = Utils.getActivityManager().getActivity(commentId);
        if (commentActivity == null) {
          LOG.warn("Cannot find Parent Comment with id '{}', it will not be displayed in notifications", commentId);
          return null;
        }
      }
      if (replyToCommentActivity == null) {
        if (StringUtils.isBlank(replyToCommentId)) {
          throw new IllegalStateException("Cannot find Reply Comment ID in context");
        }
        replyToCommentActivity = Utils.getActivityManager().getActivity(replyToCommentId);
        if (replyToCommentActivity == null) {
          LOG.warn("Cannot find Reply Comment with id '{}', it will not be displayed in notifications", replyToCommentId);
          return null;
        }
      }
      if (!Utils.getActivityManager().isNotificationEnabled(replyToCommentActivity, notification.getTo())) {
        return null;
      }

      notification.with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId());
      notification.with(SocialNotificationUtils.COMMENT_ID.getKey(), commentActivity.getId());
      notification.with(SocialNotificationUtils.COMMENT_REPLY_ID.getKey(), replyToCommentActivity.getId());

      String pluginId = notification.getKey().getId();

      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(getActivityTitle(activity), activity.getType()));
      templateContext.put("COMMENT", NotificationUtils.getNotificationActivityTitle(getActivityTitle(commentActivity), activity.getType()));
      templateContext.put("COMMENT_REPLY", isPopupOverOnly ? cutStringByMaxLength(getActivityTitle(replyToCommentActivity), 30)
                                                           : getActivityTitle(replyToCommentActivity));
      List<String>
          users = SocialNotificationUtils.mergeUsers(notification, SocialNotificationUtils.POSTER.getKey(), activity.getId(), notification.getValueOwnerParameter(SocialNotificationUtils.POSTER.getKey()));

      //
      int nbUsers = users.size();
      if (nbUsers > 0) {
        Identity lastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 1), true);
        Profile profile = lastIdentity.getProfile();
        templateContext.put("USER", Utils.addExternalFlag(lastIdentity));
        templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
        templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(lastIdentity.getRemoteId()));
        templateContext.put("NB_USERS", nbUsers);
        //
        if (nbUsers >= 2) {
          Identity beforeLastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 2), true);
          templateContext.put("LAST_USER", Utils.addExternalFlag(beforeLastIdentity));
          if (nbUsers > 2) {
            templateContext.put("COUNT", nbUsers - 2);
          }
        }
      }
      //

      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activity.getId() + "#comment-" + replyToCommentActivity.getId()));

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    private String cutStringByMaxLength(String st, int maxLength) {
      if (st == null) return st;
      st = StringEscapeUtils.unescapeHtml(st);
      if (st.length() <= maxLength) return st;
      String noHtmlSt = st.replaceAll("\\<.*?\\>", "");
      if (noHtmlSt.length() <= maxLength) return noHtmlSt;
      return noHtmlSt.substring(0, maxLength) + "...";
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
      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);

      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      if (activity == null) {
        LOG.debug("Notification related to activity with id '{}' couldn't be found. The related notification will be ignored", activityId);
        return null;
      }
      Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
      Profile profile = identity.getProfile();
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(identity.getRemoteId()));

      // In case of mention on a comment, we need provide the id of the activity, not of the comment
      String activityTitle = getActivityTitle(activity);
      if (activity.isComment()) {
        ExoSocialActivity parentActivity = Utils.getActivityManager().getParentActivity(activity);
        activityTitle = getActivityTitle(parentActivity);
        activityId = parentActivity.getId();
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activityId + "#comment-" + activity.getId()));
      } else {
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activityId));
      }
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(activityTitle, activity.getType()));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  };

  /** Defines the template builder for LikePlugin */
  private AbstractTemplateBuilder like = new LikeTemplateBuilder();

  /** Defines the template builder for LikeCommentPlugin */
  private AbstractTemplateBuilder likeComment = new LikeTemplateBuilder();

  public class LikeTemplateBuilder extends AbstractTemplateBuilder {

    @Override
    public NotificationInfo getNotificationToStore(NotificationInfo notification) {
      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      String parameterName = SocialNotificationUtils.LIKER.getKey();
      String parameterValue = notification.getValueOwnerParameter(parameterName);
      return SocialNotificationUtils.addUserToPreviousNotification(notification, parameterName, activityId, parameterValue);
    }

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);
      String pluginId = notification.getKey().getId();
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      if (activity == null) {
        LOG.debug("Notification related to activity with id '{}' couldn't be found. The related notification will be ignored", activityId);
        return null;
      }
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(getActivityTitle(activity), activity.getType()));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activity.getId()));

      if(activity.isComment()) {
        ExoSocialActivity activityOfComment = Utils.getActivityManager().getParentActivity(activity);
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activityOfComment.getId() + "#comment-" + activity.getId()));
      } else {
        templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activity.getId()));
      }
      List<String> users = SocialNotificationUtils.mergeUsers(notification, SocialNotificationUtils.LIKER.getKey(), activity.getId(), notification.getValueOwnerParameter(SocialNotificationUtils.LIKER.getKey()));
      //
      int nbUsers = users.size();
      if (nbUsers > 0) {
        Identity lastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 1), true);
        Profile profile = lastIdentity.getProfile();
        templateContext.put("USER", Utils.addExternalFlag(lastIdentity));
        templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
        templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(lastIdentity.getRemoteId()));
        templateContext.put("NB_USERS", nbUsers);
        //
        if (nbUsers >= 2) {
          Identity beforeLastIdentity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, users.get(nbUsers - 2), true);
          templateContext.put("LAST_USER", Utils.addExternalFlag(beforeLastIdentity));
          if (nbUsers > 2) {
            templateContext.put("COUNT", nbUsers - 2);
          }
        }
      }

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }
  }

  /** Defines the template builder for NewUserPlugin*/
  private AbstractTemplateBuilder newUser = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String remoteId = notification.getValueOwnerParameter(SocialNotificationUtils.REMOTE_ID.getKey());
      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, true);
      Profile userProfile = identity.getProfile();
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("PORTAL_NAME", NotificationPluginUtils.getBrandingPortalName());
      templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(identity.getRemoteId()));
      templateContext.put("AVATAR", userProfile.getAvatarUrl() != null ? userProfile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
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
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      if (activity == null) {
        LOG.debug("Activity with id '{}' doesn't exist. The related notification will be ignored", activityId);
        return null;
      }
      if (!Utils.getActivityManager().isNotificationEnabled(activity, notification.getTo())) {
        return null;
      }
      Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
      Profile profile = identity.getProfile();
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(getActivityTitle(activity), activity.getType()));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(identity.getRemoteId()));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activity.getId()));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
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
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
      ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
      if (activity == null) {
        LOG.debug("Notification related to activity with id '{}' couldn't be found. The related notification will be ignored", activityId);
        return null;
      }
      if (!Utils.getActivityManager().isNotificationEnabled(activity, notification.getTo())) {
        return null;
      }
      Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
      Profile profile = identity.getProfile();
      Identity spaceIdentity = Utils.getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner(), true);
      Space space = Utils.getSpaceService().getSpaceByPrettyName(spaceIdentity.getRemoteId());
      if (space == null) {
        return null;
      }
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("ACTIVITY", NotificationUtils.getNotificationActivityTitle(getActivityTitle(activity), activity.getType()));
      templateContext.put("SPACE", space.getDisplayName());
      templateContext.put("SPACE_URL", LinkProvider.getActivityUriForSpace(space.getPrettyName(), space.getGroupId().replace("/spaces/", "")));
      templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(identity.getRemoteId()));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProvider.getSingleActivityUrl(activity.getId()));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
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
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String sender = notification.getValueOwnerParameter("sender");
      String status = notification.getValueOwnerParameter("status");
      String toUser = notification.getTo();
      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, sender, true);
      Profile userProfile = identity.getProfile();
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("PROFILE_URL", LinkProvider.getUserProfileUri(identity.getRemoteId()));
      templateContext.put("AVATAR", userProfile.getAvatarUrl() != null ? userProfile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("ACCEPT_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(ACCEPT_INVITATION_TO_CONNECT, sender, toUser, notification.getId(), MESSAGE_JSON_FILE_NAME));
      templateContext.put("REFUSE_CONNECTION_REQUEST_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(REFUSE_INVITATION_TO_CONNECT, sender, toUser, notification.getId(), MESSAGE_JSON_FILE_NAME));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
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
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String status = notification.getValueOwnerParameter("status");
      String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
      Space space = Utils.getSpaceService().getSpaceById(spaceId);
      if (space == null) {
        return null;
      }
      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getValueOwnerParameter("request_from"), true);
      Profile userProfile = identity.getProfile();
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("SPACE", space.getDisplayName());
      templateContext.put("USER", Utils.addExternalFlag(identity));
      templateContext.put("SPACE_URL", LinkProvider.getActivityUriForSpace(space.getPrettyName(), space.getGroupId().replace("/spaces/", "")));
      templateContext.put("AVATAR", userProfile.getAvatarUrl() != null ? userProfile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("VALIDATE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(VALIDATE_SPACE_REQUEST, space.getId(), identity.getRemoteId()) + "/" + notification.getTo() + "/" + notification.getId() + "/" + MESSAGE_JSON_FILE_NAME);
      templateContext.put("REFUSE_SPACE_REQUEST_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(REFUSE_SPACE_REQUEST, space.getId(), identity.getRemoteId()) + "/" + notification.getTo() + "/" + notification.getId() + "/" + MESSAGE_JSON_FILE_NAME);
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
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
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      String status = notification.getValueOwnerParameter("status");
      String spaceId = notification.getValueOwnerParameter(SocialNotificationUtils.SPACE_ID.getKey());
      String sender = notification.getValueOwnerParameter(SocialNotificationUtils.SENDER.getKey());

      Space space = Utils.getSpaceService().getSpaceById(spaceId);
      if (space == null) {
        return null;
      }
      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("READ",
                          Boolean.TRUE.equals(Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey()))) ? "read" : "unread");
      templateContext.put("STATUS", status != null && status.equals("accepted") ? "ACCEPTED" : "PENDING");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("SPACE", space.getDisplayName());
      templateContext.put("SENDER_NAME", sender);
      templateContext.put("SPACE_URL", LinkProvider.getActivityUriForSpace(space.getPrettyName(), space.getGroupId().replace("/spaces/", "")));
      templateContext.put("SPACE_AVATAR", space.getAvatarUrl() != null ? space.getAvatarUrl() : LinkProvider.SPACE_DEFAULT_AVATAR_URL);
      templateContext.put("ACCEPT_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(ACCEPT_SPACE_INVITATION, space.getId(), notification.getTo(), notification.getId(), MESSAGE_JSON_FILE_NAME));
      templateContext.put("REFUSE_SPACE_INVITATION_ACTION_URL", LinkProviderUtils.getWebNotificationRestUrl(REFUSE_SPACE_INVITATION, space.getId(), notification.getTo(), notification.getId(), MESSAGE_JSON_FILE_NAME));
      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }

  };

  public PushTemplateProvider(InitParams initParams) {
    super(initParams);
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

  private String getActivityTitle(ExoSocialActivity activity) {
    return Utils.getActivityManager().getActivityTitle(activity);
  }

}
