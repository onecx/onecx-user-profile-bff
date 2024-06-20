package org.tkit.onecx.user.profile.bff.rs.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.UserProfileMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.image.bff.clients.api.AvatarInternalApi;
import gen.org.tkit.onecx.image.bff.clients.model.ImageInfo;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.UserAvatarApiService;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.RefTypeDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserAvatarRestController implements UserAvatarApiService {

    @Inject
    @RestClient
    AvatarInternalApi client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    HttpHeaders headers;

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        return exceptionMapper.restException(ex);
    }

    @Override
    public Response deleteUserAvatar() {
        try (Response response = client.deleteMyImage()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getUserAvatar(RefTypeDTO refType) {
        Response.ResponseBuilder responseBuilder = null;
        try (Response response = client.getMyImage(mapper.map(refType))) {
            var contentType = response.getHeaderString(HttpHeaders.CONTENT_TYPE);
            var contentLength = response.getHeaderString(HttpHeaders.CONTENT_LENGTH);
            var body = response.readEntity(byte[].class);

            if (contentType != null && body.length != 0) {
                responseBuilder = Response.status(response.getStatus())
                        .header(HttpHeaders.CONTENT_TYPE, contentType)
                        .header(HttpHeaders.CONTENT_LENGTH, contentLength)
                        .entity(body);
            } else {
                responseBuilder = Response.status(Response.Status.BAD_REQUEST);
            }

            return responseBuilder.build();
        }

    }

    @Override
    public Response updateAvatar(RefTypeDTO refType, byte[] body) {
        try (Response response = client.updateMyImage(mapper.map(refType), body, headers.getLength())) {
            var imageInfo = response.readEntity(ImageInfo.class);

            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }

    @Override
    public Response uploadAvatar(RefTypeDTO refType, byte[] body) {
        try (Response response = client.uploadMyImage(headers.getLength(), mapper.map(refType), body)) {
            var imageInfo = response.readEntity(ImageInfo.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }
}
