package org.exoplatform.push.rest;

import io.swagger.annotations.*;
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
@Api(tags = "/v1/messaging/device", value = "/v1/messaging/device", description = "Managing devices for Push Notifications")
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
  @ApiOperation(value = "Gets a device by token",
          httpMethod = "GET",
          response = Response.class,
          notes = "This returns the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Device returned"),
          @ApiResponse (code = 400, message = "Invalid query input - token is not valid"),
          @ApiResponse (code = 401, message = "Not authorized to get the device linked to the token"),
          @ApiResponse (code = 500, message = "Internal server error")})
  public Response getDevice(@ApiParam(value = "Token", required = true) @PathParam("token") String token) {
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
  @ApiOperation(value = "Creates or updates a device",
          httpMethod = "POST",
          response = Response.class,
          notes = "This creates or updates the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Device created"),
          @ApiResponse (code = 400, message = "Invalid query input"),
          @ApiResponse (code = 401, message = "Not authorized to create the device"),
          @ApiResponse (code = 500, message = "Internal server error")})
  public Response saveDevice(@ApiParam(value = "Device", required = true) Device device) {
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
  @ApiOperation(value = "Deletes a device",
          httpMethod = "DELETE",
          response = Response.class,
          notes = "This deletes the device in the following cases: <br/><ul><li>the owner of the device is the authenticated user</li><li>the authenticated user is the super user</li></ul>")
  @ApiResponses(value = {
          @ApiResponse(code = 200, message = "Device deleted"),
          @ApiResponse (code = 400, message = "Invalid query input"),
          @ApiResponse (code = 401, message = "Not authorized to delete the device"),
          @ApiResponse (code = 500, message = "Internal server error")})
  public Response deleteDevice(@ApiParam(value = "Token", required = true) @PathParam("token") String token) {
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
