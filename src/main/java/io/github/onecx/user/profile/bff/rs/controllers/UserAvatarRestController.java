package io.github.onecx.user.profile.bff.rs.controllers;

import java.io.File;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.io.github.onecx.user.profile.bff.clients.api.AvatarV1Api;
import gen.io.github.onecx.user.profile.bff.clients.model.ImageInfo;
import gen.io.github.onecx.user.profile.bff.clients.model.ProblemDetailResponse;
import gen.io.github.onecx.user.profile.bff.rs.internal.UserAvatarApiService;
import io.github.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import io.github.onecx.user.profile.bff.rs.mappers.ProblemDetailMapper;
import io.github.onecx.user.profile.bff.rs.mappers.UserProfileMapper;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserAvatarRestController implements UserAvatarApiService {

    @Inject
    @RestClient
    AvatarV1Api client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ProblemDetailMapper problemDetailMapper;

    @Override
    public Response deleteUserAvatar() {
        try (Response response = client.deleteUserAvatar()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getUserAvatar(String id) {
        try (Response response = client.getUserAvatar(id)) {
            var byteResponse = response.readEntity(byte[].class);
            return Response.status(response.getStatus())
                    .entity(byteResponse).build();
        }
    }

    @Override
    public Response getUserAvatarInfo() {
        try (Response response = client.getUserAvatarInfo()) {
            var imageInfo = response.readEntity(ImageInfo.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }

    @Override
    public Response uploadAvatar(File body) {
        try (Response response = client.uploadAvatar(body)) {
            var imageInfo = response.readEntity(ImageInfo.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(imageInfo)).build();
        }
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        // if client response is bad request remap bad request to DTO
        if (ex.getResponse().getStatus() == RestResponse.StatusCode.BAD_REQUEST) {
            try {
                var clientError = ex.getResponse().readEntity(ProblemDetailResponse.class);
                return Response.status(ex.getResponse().getStatus()).type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(problemDetailMapper.map(clientError)).build();
            } catch (Exception e) {
                // ignore error the bad request has not problem detail response object
                return Response.status(ex.getResponse().getStatus()).build();
            }
        } else {
            return Response.status(ex.getResponse().getStatus()).build();
        }
    }
}
