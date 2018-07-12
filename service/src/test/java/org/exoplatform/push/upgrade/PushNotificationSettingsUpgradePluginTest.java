package org.exoplatform.push.upgrade;

import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.notification.channel.WebChannel;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.push.channel.PushChannel;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.UserImpl;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.Arrays;

import static org.exoplatform.commons.notification.impl.AbstractService.EXO_IS_ACTIVE;
import static org.mockito.Mockito.*;

public class PushNotificationSettingsUpgradePluginTest {

    @Test
    public void shouldNotUpdateSettingsWhenNoNotificationsSettings() throws Exception {
        // Given
        SettingService settingService = mock(SettingService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        UserHandler userHandler = mock(UserHandler.class);
        when(userHandler.findAllUsers()).thenReturn(new ListAccessImpl<>(User.class, Arrays.asList(
                new UserImpl("john"),
                new UserImpl("mary")
        )));
        when(organizationService.getUserHandler()).thenReturn(userHandler);

        UserSettingService userSettingService = mock(UserSettingService.class);
        UserSetting userSetting = mock(UserSetting.class);
        when(userSettingService.get(anyString())).thenReturn(userSetting);

        PluginSettingService pluginSettingService = mock(PluginSettingService.class);
        EntityManagerService entityManagerService = mock(EntityManagerService.class);

        InitParams initParams = new InitParams();
        PushNotificationSettingsUpgradePlugin pushNotificationSettingsUpgradePlugin = new PushNotificationSettingsUpgradePlugin(settingService, userSettingService, pluginSettingService, entityManagerService, initParams);

        // When
        pushNotificationSettingsUpgradePlugin.processUpgrade("5.0.0", "5.1.0");

        // Then
        verify(userSetting, never()).setChannelActive(anyString());
        verify(userSetting, never()).removeChannelActive(anyString());
        verify(userSetting, never()).setChannelPlugins(anyString(), anyList());
    }

    @Test
    public void shouldNotUpdateSettingsWhenPushChannelAlreadyConfigured() throws Exception {
        // Given
        SettingService settingService = mock(SettingService.class);
        when(settingService.get(any(Context.class), eq(Scope.APPLICATION.id("NOTIFICATION")), eq(EXO_IS_ACTIVE))).thenReturn(new SettingValue("{WEB_CHANNEL}"));
        when(settingService.get(any(Context.class), eq(Scope.APPLICATION.id("NOTIFICATION")), eq("exo:PUSH_CHANNELChannel"))).thenReturn(new SettingValue("{ActivityReplyToCommentPlugin},{ActivityCommentPlugin}"));
        OrganizationService organizationService = mock(OrganizationService.class);
        UserHandler userHandler = mock(UserHandler.class);
        when(userHandler.findAllUsers()).thenReturn(new ListAccessImpl<>(User.class, Arrays.asList(
                new UserImpl("john"),
                new UserImpl("mary")
        )));
        when(organizationService.getUserHandler()).thenReturn(userHandler);

        UserSettingService userSettingService = mock(UserSettingService.class);
        UserSetting userSetting = mock(UserSetting.class);
        when(userSettingService.get(anyString())).thenReturn(userSetting);

        PluginSettingService pluginSettingService = mock(PluginSettingService.class);
        EntityManagerService entityManagerService = mock(EntityManagerService.class);

        InitParams initParams = new InitParams();
        PushNotificationSettingsUpgradePlugin pushNotificationSettingsUpgradePlugin = new PushNotificationSettingsUpgradePlugin(settingService, userSettingService, pluginSettingService, entityManagerService, initParams);

        // When
        pushNotificationSettingsUpgradePlugin.processUpgrade("5.0.0", "5.1.0");

        // Then
        verify(userSetting, never()).setChannelActive(anyString());
        verify(userSetting, never()).removeChannelActive(anyString());
        verify(userSetting, never()).setChannelPlugins(anyString(), anyList());
    }

    @Test
    public void shouldUpdateSettingsWhenPushChannelNotConfiguredAndWebChannelActivated() throws Exception {
        // Given
        int nbOfUsers = 2;
        SettingService settingService = mock(SettingService.class);
        when(settingService.getContextsByTypeAndScopeAndSettingName(eq(Context.USER.getName()), eq(Scope.APPLICATION.getName()), eq("NOTIFICATION"), eq(EXO_IS_ACTIVE), eq(0), anyInt()))
                .thenReturn(Arrays.asList(Context.USER.id("john"), Context.USER.id("mary")));
        when(settingService.getContextsByTypeAndScopeAndSettingName(eq(Context.USER.getName()), eq(Scope.APPLICATION.getName()), eq("NOTIFICATION"), eq(EXO_IS_ACTIVE), eq(nbOfUsers), anyInt()))
                .thenReturn(null);
        when(settingService.get(any(Context.class), eq(Scope.APPLICATION.id("NOTIFICATION")), eq("exo:PUSH_CHANNELChannel"))).thenReturn(null);
        OrganizationService organizationService = mock(OrganizationService.class);
        UserHandler userHandler = mock(UserHandler.class);
        when(userHandler.findUserByName(eq("john"), eq(UserStatus.ANY))).thenReturn(new UserImpl("john"));
        when(userHandler.findUserByName(eq("mary"), eq(UserStatus.ANY))).thenReturn(new UserImpl("mary"));
        when(organizationService.getUserHandler()).thenReturn(userHandler);

        UserSettingService userSettingService = mock(UserSettingService.class);

        UserSetting userSetting = mock(UserSetting.class);
        when(userSetting.isChannelActive(eq(WebChannel.ID))).thenReturn(true);
        when(userSettingService.get(anyString())).thenReturn(userSetting);

        PluginSettingService pluginSettingService = mock(PluginSettingService.class);
        EntityManagerService entityManagerService = mock(EntityManagerService.class);

        InitParams initParams = new InitParams();
        PushNotificationSettingsUpgradePlugin pushNotificationSettingsUpgradePlugin = new PushNotificationSettingsUpgradePlugin(settingService, userSettingService, pluginSettingService, entityManagerService, initParams);

        // When
        pushNotificationSettingsUpgradePlugin.processUpgrade("5.0.0", "5.1.0");

        // Then
        verify(userSetting, times(nbOfUsers)).setChannelActive(eq(PushChannel.ID));
        verify(userSetting, never()).removeChannelActive(anyString());
        verify(userSetting, times(nbOfUsers)).setChannelPlugins(eq(PushChannel.ID), anyList());
    }

    @Test
    public void shouldUpdateSettingsWhenPushChannelNotConfiguredAndWebChannelNotActivated() throws Exception {
        // Given
        int nbOfUsers = 2;
        SettingService settingService = mock(SettingService.class);
        when(settingService.getContextsByTypeAndScopeAndSettingName(eq(Context.USER.getName()), eq(Scope.APPLICATION.getName()), eq("NOTIFICATION"), eq(EXO_IS_ACTIVE), eq(0), anyInt()))
                .thenReturn(Arrays.asList(Context.USER.id("john"), Context.USER.id("mary")));
        when(settingService.getContextsByTypeAndScopeAndSettingName(eq(Context.USER.getName()), eq(Scope.APPLICATION.getName()), eq("NOTIFICATION"), eq(EXO_IS_ACTIVE), eq(nbOfUsers), anyInt()))
                .thenReturn(null);
        when(settingService.get(any(Context.class), eq(Scope.APPLICATION.id("NOTIFICATION")), eq(EXO_IS_ACTIVE))).thenReturn(new SettingValue("{WEB_CHANNEL}"));
        when(settingService.get(any(Context.class), eq(Scope.APPLICATION.id("NOTIFICATION")), eq("exo:PUSH_CHANNELChannel"))).thenReturn(null);
        OrganizationService organizationService = mock(OrganizationService.class);
        UserHandler userHandler = mock(UserHandler.class);
        when(userHandler.findUserByName(eq("john"), eq(UserStatus.ANY))).thenReturn(new UserImpl("john"));
        when(userHandler.findUserByName(eq("mary"), eq(UserStatus.ANY))).thenReturn(new UserImpl("mary"));
        when(organizationService.getUserHandler()).thenReturn(userHandler);

        UserSettingService userSettingService = mock(UserSettingService.class);

        UserSetting userSetting = mock(UserSetting.class);
        when(userSetting.isChannelActive(eq(WebChannel.ID))).thenReturn(false);
        when(userSettingService.get(anyString())).thenReturn(userSetting);

        PluginSettingService pluginSettingService = mock(PluginSettingService.class);
        EntityManagerService entityManagerService = mock(EntityManagerService.class);

        InitParams initParams = new InitParams();
        PushNotificationSettingsUpgradePlugin pushNotificationSettingsUpgradePlugin = new PushNotificationSettingsUpgradePlugin(settingService, userSettingService, pluginSettingService, entityManagerService, initParams);

        // When
        pushNotificationSettingsUpgradePlugin.processUpgrade("5.0.0", "5.1.0");

        // Then
        verify(userSetting, never()).setChannelActive(eq(PushChannel.ID));
        verify(userSetting, times(nbOfUsers)).removeChannelActive(anyString());
        verify(userSetting, times(nbOfUsers)).setChannelPlugins(eq(PushChannel.ID), anyList());
    }
}