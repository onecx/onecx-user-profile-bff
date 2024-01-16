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

import gen.io.github.onecx.user.profile.bff.clients.api.UserProfileV1Api;
import gen.io.github.onecx.user.profile.bff.clients.model.*;
import gen.io.github.onecx.user.profile.bff.rs.internal.UserProfileApiService;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.CreateUserPreferenceDTO;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.UpdateUserPersonDTO;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.UpdateUserSettingsDTO;
import io.github.onecx.user.profile.bff.rs.mappers.ExceptionMapper;
import io.github.onecx.user.profile.bff.rs.mappers.ProblemDetailMapper;
import io.github.onecx.user.profile.bff.rs.mappers.UserProfileMapper;

@ApplicationScoped
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
@LogService
public class UserProfileRestController implements UserProfileApiService {

    @Inject
    @RestClient
    UserProfileV1Api client;

    @Inject
    UserProfileMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    ProblemDetailMapper problemDetailMapper;

    @Override
    public Response createUserPreference(CreateUserPreferenceDTO createUserPreferenceDTO) {
        try (Response response = client.createUserPreference(mapper.map(createUserPreferenceDTO))) {
            var preferenceResponse = response.readEntity(UserPreference.class);
            return Response.status(response.getStatus())
                    .location(response.getLocation())
                    .entity(mapper.map(preferenceResponse)).build();
        }
    }

    @Override
    public Response deleteMyUserProfile() {
        try (Response response = client.deleteUserProfile()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response deleteUserPreference(String id) {
        try (Response response = client.deleteUserPreference(id)) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response getMyUserProfile() {
        try (Response response = client.getUserProfile()) {
            var profileResponse = response.readEntity(UserProfile.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(profileResponse)).build();
        }
    }

    @Override
    public Response getUserPerson() {
        try (Response response = client.getUserPerson()) {
            var userPerson = response.readEntity(UserPerson.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPerson)).build();
        }
    }

    @Override
    public Response getUserPreference() {
        try (Response response = client.getUserPreference()) {
            var userPreferences = response.readEntity(UserPreferences.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPreferences)).build();
        }
    }

    @Override
    public Response getUserSettings() {
        try (Response response = client.getUserSettings()) {
            var userProfileAccountSettings = response.readEntity(UserProfileAccountSettings.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userProfileAccountSettings)).build();
        }
    }

    @Override
    public Response updateUserPerson(UpdateUserPersonDTO updateUserPersonDTO) {
        try (Response response = client.updateUserPerson(mapper.map(updateUserPersonDTO))) {
            var userPerson = response.readEntity(UserPerson.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPerson)).build();
        }
    }

    @Override
    public Response updateUserPreference(String id, String body) {
        try (Response response = client.updateUserPreference(id, body)) {
            var userPreference = response.readEntity(UserPreference.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPreference)).build();
        }
    }

    @Override
    public Response updateUserSettings(UpdateUserSettingsDTO updateUserSettingsDTO) {
        try (Response response = client.updateUserSettings(mapper.map(updateUserSettingsDTO))) {
            var userProfileAccountSettings = response.readEntity(UserProfileAccountSettings.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userProfileAccountSettings)).build();
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
