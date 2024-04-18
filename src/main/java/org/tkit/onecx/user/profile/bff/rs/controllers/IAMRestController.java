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
import org.tkit.onecx.user.profile.bff.rs.mappers.IAMMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.iam.kc.client.api.AdminUserControllerApi;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.IamApiService;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UserResetPasswordRequestDTO;

@ApplicationScoped
@LogService
@Transactional(value = Transactional.TxType.NOT_SUPPORTED)
public class IAMRestController implements IamApiService {

    @Inject
    @RestClient
    AdminUserControllerApi adminUserControllerApi;

    @Inject
    IAMMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response resetPassword(UserResetPasswordRequestDTO userResetPasswordRequestDTO) {
        try (Response response = adminUserControllerApi.resetPassword(mapper.map(userResetPasswordRequestDTO))) {
            return Response.status(response.getStatus()).build();
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
