package org.tkit.onecx.user.profile.bff.rs.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import org.tkit.onecx.user.profile.bff.rs.mappers.UserProfileMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.user.profile.bff.clients.api.UserProfileApi;
import gen.org.tkit.onecx.user.profile.bff.clients.model.*;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.UserProfileApiService;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UpdateUserProfileRequestDTO;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserProfileRestController implements UserProfileApiService {

    @Inject
    @RestClient
    UserProfileApi client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response deleteMyUserProfile() {
        try (Response response = client.deleteMyUserProfile()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getMyUserProfile() {
        try (Response response = client.getMyUserProfile()) {
            var profileResponse = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(profileResponse)).build();
        }
    }

    @Override
    public Response updateMyUserProfile(UpdateUserProfileRequestDTO updateUserProfileRequestDTO) {
        try (Response response = client.updateMyUserProfile(mapper.mapUpdate(updateUserProfileRequestDTO))) {
            var updateResponse = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(updateResponse)).build();
        }
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        return exceptionMapper.restException(ex);
    }
}
