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

import gen.org.tkit.onecx.user.profile.bff.clients.api.UserProfileAdminApi;
import gen.org.tkit.onecx.user.profile.bff.clients.model.UserProfile;
import gen.org.tkit.onecx.user.profile.bff.clients.model.UserProfilePageResult;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.UserProfileAdminApiService;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UpdateUserPersonRequestDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UserPersonCriteriaDTO;

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

    @Override
    public Response deleteUserProfile(String id) {
        try (Response response = client.deleteUserProfileData(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getUserProfile(String id) {
        try (Response response = client.getUserProfileData(id)) {
            var userProfile = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus()).entity(mapper.map(userProfile)).build();
        }
    }

    @Override
    public Response searchUserProfile(UserPersonCriteriaDTO userPersonCriteriaDTO) {
        try (Response response = client.searchUserProfileData(mapper.map(userPersonCriteriaDTO))) {
            var userProfilePageResult = response.readEntity(UserProfilePageResult.class);
            return Response.status(response.getStatus()).entity(mapper.map(userProfilePageResult)).build();
        }
    }

    @Override
    public Response updateUserProfile(String id, UpdateUserPersonRequestDTO updateUserPersonRequestDTO) {
        try (Response response = client.updateUserProfileData(id, mapper.map(updateUserPersonRequestDTO))) {
            var userProfile = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus()).entity(mapper.map(userProfile)).build();
        }
    }

    @ServerExceptionMapper
    public Response restException(WebApplicationException ex) {
        return exceptionMapper.restException(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

}
