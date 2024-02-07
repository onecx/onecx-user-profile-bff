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
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.CreateUserPreferenceDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UpdateUserPersonDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UpdateUserSettingsDTO;

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
    public Response createUserPreference(CreateUserPreferenceDTO createUserPreferenceDTO) {
        try (Response response = client.createUserProfilePreference(mapper.map(createUserPreferenceDTO))) {
            var preferenceResponse = response.readEntity(UserPreference.class);
            return Response.status(response.getStatus())
                    .location(response.getLocation())
                    .entity(mapper.map(preferenceResponse)).build();
        }
    }

    @Override
    public Response deleteMyUserProfile() {
        try (Response response = client.deleteMyUserProfile()) {
            return Response.status(response.getStatus()).build();
        }
    }

    @Override
    public Response deleteUserPreference(String id) {
        try (Response response = client.deleteUserProfilePreference(id)) {
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
    public Response getUserPerson() {
        try (Response response = client.getUserProfilePerson()) {
            var userPerson = response.readEntity(UserPerson.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPerson)).build();
        }
    }

    @Override
    public Response getUserPreference() {
        try (Response response = client.getUserProfilePreference()) {
            var userPreferences = response.readEntity(UserPreferences.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPreferences)).build();
        }
    }

    @Override
    public Response getUserSettings() {
        try (Response response = client.getUserProfileSettings()) {
            var userProfileAccountSettings = response.readEntity(UserProfileAccountSettings.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userProfileAccountSettings)).build();
        }
    }

    @Override
    public Response updateUserPerson(UpdateUserPersonDTO updateUserPersonDTO) {
        try (Response response = client.updateUserProfilePerson(mapper.map(updateUserPersonDTO))) {
            var userPerson = response.readEntity(UserPerson.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPerson)).build();
        }
    }

    @Override
    public Response updateUserPreference(String id, String body) {
        try (Response response = client.updateUserProfilePreference(id, body)) {
            var userPreference = response.readEntity(UserPreference.class);
            return Response.status(response.getStatus())
                    .entity(mapper.map(userPreference)).build();
        }
    }

    @Override
    public Response updateUserSettings(UpdateUserSettingsDTO updateUserSettingsDTO) {
        try (Response response = client.updateUserProfileSettings(mapper.map(updateUserSettingsDTO))) {
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
        return exceptionMapper.restException(ex);
    }
}
