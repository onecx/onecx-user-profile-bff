package org.tkit.onecx.user.profile.bff.rs.controllers;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.tkit.onecx.user.profile.bff.rs.mappers.IAMMapper;

import gen.org.tkit.onecx.iam.kc.client.api.AdminUserControllerApi;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.IamApiService;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UserResetPasswordRequestDTO;

public class IAMRestController implements IamApiService {

    @Inject
    @RestClient
    AdminUserControllerApi adminUserControllerApi;

    @Inject
    IAMMapper mapper;

    @Override
    public Response resetPassword(UserResetPasswordRequestDTO userResetPasswordRequestDTO) {
        try (Response response = adminUserControllerApi.resetPassword(mapper.map(userResetPasswordRequestDTO))) {
            return Response.status(response.getStatus()).build();
        }
    }
}
