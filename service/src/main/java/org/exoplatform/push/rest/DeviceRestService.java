package org.exoplatform.push.rest;

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
  public Response getDevice(@PathParam("token") String token) {
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

    return Response.ok(device).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response createDevice(Device device) {
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

    deviceService.createDevice(device);

    return Response.ok().build();
  }

  @DELETE
  @Path("{token}")
  @RolesAllowed("users")
  public Response deleteDevice(@PathParam("token") String token) {
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
