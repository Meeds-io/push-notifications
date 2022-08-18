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
package org.exoplatform.push.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.push.domain.Device;
import org.exoplatform.push.service.DeviceService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1/messaging/device")
@Tag(name = "/v1/messaging/device", description = "Managing devices for Push Notifications")
public class DeviceRestService implements ResourceContainer {

  private DeviceService deviceService;

  private UserACL userACL;

  public DeviceRestService(DeviceService deviceService, UserACL userACL) {
    this.deviceService = deviceService;
    this.userACL = userACL;
  }

  @GET
  @Path("{token}")
  @RolesAllowed("users")
  @Operation(summary = "Gets a device by token",
          method = "GET",
          description = "This returns the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Device returned"),
          @ApiResponse (responseCode = "400", description = "Invalid query input - token is not valid"),
          @ApiResponse (responseCode = "401", description = "Not authorized to get the device linked to the token"),
          @ApiResponse (responseCode = "500", description = "Internal server error")})
  public Response getDevice(@Parameter(description = "Token", required = true) @PathParam("token") String token) {
    if(StringUtils.isBlank(token)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    ConversationState conversationState = ConversationState.getCurrent();
    if(conversationState == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    Device device = deviceService.getDeviceByToken(token);

    if(device == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    String authenticatedUser = conversationState.getIdentity().getUserId();
    if(StringUtils.isEmpty(authenticatedUser)
            || (!authenticatedUser.equals(device.getUsername()) && !authenticatedUser.equals(userACL.getSuperUser()))) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    return Response.ok(device, MediaType.APPLICATION_JSON_TYPE).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Creates or updates a device",
          method = "POST",
          description = "This creates or updates the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Device created"),
          @ApiResponse (responseCode = "400", description = "Invalid query input"),
          @ApiResponse (responseCode = "401", description = "Not authorized to create the device"),
          @ApiResponse (responseCode = "500", description = "Internal server error")})
  public Response saveDevice(@Parameter(description = "Device", required = true) Device device) {
    if(device == null || StringUtils.isBlank(device.getToken()) || StringUtils.isBlank(device.getUsername())) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    ConversationState conversationState = ConversationState.getCurrent();
    if(conversationState == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }
    String authenticatedUser = conversationState.getIdentity().getUserId();
    if(StringUtils.isEmpty(authenticatedUser)
            || (!authenticatedUser.equals(device.getUsername()) && !authenticatedUser.equals(userACL.getSuperUser()))) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    deviceService.saveDevice(device);

    return Response.ok().build();
  }

  @DELETE
  @Path("{token}")
  @RolesAllowed("users")
  @Operation(summary = "Deletes a device",
          method = "DELETE",
          description = "This deletes the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Device deleted"),
          @ApiResponse (responseCode = "400", description = "Invalid query input"),
          @ApiResponse (responseCode = "401", description = "Not authorized to delete the device"),
          @ApiResponse (responseCode = "500", description = "Internal server error")})
  public Response deleteDevice(@Parameter(description = "Token", required = true) @PathParam("token") String token) {
    if(StringUtils.isBlank(token)) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    ConversationState conversationState = ConversationState.getCurrent();
    if(conversationState == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    Device device = deviceService.getDeviceByToken(token);

    if(device == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }

    String authenticatedUser = conversationState.getIdentity().getUserId();
    if(StringUtils.isEmpty(authenticatedUser)
            || (!authenticatedUser.equals(device.getUsername()) && !authenticatedUser.equals(userACL.getSuperUser()))) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    deviceService.deleteDevice(device);

    return Response.ok().build();
  }
}
