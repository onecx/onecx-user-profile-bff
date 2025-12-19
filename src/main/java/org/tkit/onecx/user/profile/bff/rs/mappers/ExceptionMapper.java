package org.tkit.onecx.user.profile.bff.rs.mappers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.user.profile.bff.clients.model.ProblemDetailResponse;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailInvalidParamDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailParamDTO;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.ProblemDetailResponseDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface ExceptionMapper {

    default Response restException(WebApplicationException ex) {
        if (ex.getResponse().getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else if (ex.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (ex.getResponse().getStatus() == RestResponse.StatusCode.BAD_REQUEST) {
            try {
                return Response.status(ex.getResponse().getStatus())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .entity(map(ex.getResponse().readEntity(ProblemDetailResponse.class))).build();
            } catch (Exception e) {
                // ignore error the bad request has no problem detail response object
                return Response.status(ex.getResponse().getStatus()).build();
            }
        } else {
            return Response.status(ex.getResponse().getStatus()).build();
        }
    }

    @Mapping(target = "removeParamsItem", ignore = true)
    @Mapping(target = "removeInvalidParamsItem", ignore = true)
    ProblemDetailResponseDTO map(ProblemDetailResponse problemDetailResponse);

    default RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        var dto = exception("CONSTRAINT_VIOLATIONS", ex.getMessage());
        dto.setInvalidParams(createErrorValidationResponse(ex.getConstraintViolations()));
        return RestResponse.status(Response.Status.BAD_REQUEST, dto);
    }

    @Mapping(target = "removeParamsItem", ignore = true)
    @Mapping(target = "params", ignore = true)
    @Mapping(target = "invalidParams", ignore = true)
    @Mapping(target = "removeInvalidParamsItem", ignore = true)
    ProblemDetailResponseDTO exception(String errorCode, String detail);

    default List<ProblemDetailParamDTO> map(Map<String, Object> params) {
        if (params == null) {
            return List.of();
        }
        return params.entrySet().stream().map(e -> {
            var item = new ProblemDetailParamDTO();
            item.setKey(e.getKey());
            if (e.getValue() != null) {
                item.setValue(e.getValue().toString());
            }
            return item;
        }).toList();
    }

    List<ProblemDetailInvalidParamDTO> createErrorValidationResponse(
            Set<ConstraintViolation<?>> constraintViolation);

    @Mapping(target = "name", source = "propertyPath")
    @Mapping(target = "message", source = "message")
    ProblemDetailInvalidParamDTO createError(ConstraintViolation<?> constraintViolation);

    default String mapPath(Path path) {
        return path.toString();
    }
}
