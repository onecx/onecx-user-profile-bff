package io.github.onecx.user.profile.bff.rs.controllers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.io.github.onecx.user.profile.bff.clients.api.UserProfileAdminApi;
import gen.io.github.onecx.user.profile.bff.clients.model.ProblemDetailResponse;
import gen.io.github.onecx.user.profile.bff.clients.model.UserProfile;
import gen.io.github.onecx.user.profile.bff.clients.model.UserProfilePageResult;
import gen.io.github.onecx.user.profile.bff.rs.internal.UserProfileAdminApiService;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.UpdateUserPersonRequestDTO;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.UserPersonCriteriaDTO;
import io.github.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import io.github.onecx.user.profile.bff.rs.mappers.ProblemDetailMapper;
import io.github.onecx.user.profile.bff.rs.mappers.UserProfileMapper;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserProfileAdminRestController implements UserProfileAdminApiService {

    @Inject
    @RestClient
    UserProfileAdminApi client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ProblemDetailMapper problemDetailMapper;

    @Override
    public Response deleteUserProfile(String id) {
        try (Response response = client.deleteUserProfile(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getUserProfile(String id) {
        try (Response response = client.getUserProfile(id)) {
            var userProfile = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus()).entity(mapper.map(userProfile)).build();
        }
    }

    @Override
    public Response searchUserProfile(UserPersonCriteriaDTO userPersonCriteriaDTO) {
        try (Response response = client.searchUserProfile(mapper.map(userPersonCriteriaDTO))) {
            var userProfilePageResult = response.readEntity(UserProfilePageResult.class);
            return Response.status(response.getStatus()).entity(mapper.map(userProfilePageResult)).build();
        }
    }

    @Override
    public Response updateUserProfile(String id, UpdateUserPersonRequestDTO updateUserPersonRequestDTO) {
        try (Response response = client.updateUserProfile(id, mapper.map(updateUserPersonRequestDTO))) {
            return Response.status(response.getStatus()).build();
        }
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        // if client response is bad request remap bad request to DTO
        if (ex.getResponse().getStatus() == RestResponse.StatusCode.BAD_REQUEST) {
            try {
                var clientError = ex.getResponse().readEntity(ProblemDetailResponse.class);
                return Response.status(ex.getResponse().getStatus())
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
