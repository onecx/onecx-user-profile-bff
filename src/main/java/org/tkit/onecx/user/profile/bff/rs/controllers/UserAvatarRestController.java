package org.tkit.onecx.user.profile.bff.rs.controllers;

import java.io.File;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.UserProfileMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.clients.api.AvatarApi;
import gen.org.tkit.onecx.user.profile.bff.clients.model.ImageInfo;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.UserAvatarApiService;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserAvatarRestController implements UserAvatarApiService {

    @Inject
    @RestClient
    AvatarApi client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response deleteUserAvatar() {
        try (Response response = client.deleteUserProfileAvatar()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getUserAvatar(String id) {
        try (Response response = client.getUserProfileAvatar(id)) {
            var byteResponse = response.readEntity(byte[].class);
            return Response.status(response.getStatus())
                    .entity(byteResponse).build();
        }
    }

    @Override
    public Response getUserAvatarInfo() {
        try (Response response = client.getUserProfileAvatarInfo()) {
            var imageInfo = response.readEntity(ImageInfo.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }

    @Override
    public Response uploadAvatar(File body) {
        try (Response response = client.uploadUserProfileAvatar(body)) {
            var imageInfo = response.readEntity(ImageInfo.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        return exceptionMapper.restException(ex);
    }
}
