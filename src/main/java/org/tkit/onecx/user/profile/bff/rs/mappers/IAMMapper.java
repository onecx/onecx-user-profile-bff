package org.tkit.onecx.user.profile.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.iam.kc.client.model.UserResetPasswordRequest;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.UserResetPasswordRequestDTO;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface IAMMapper {

    UserResetPasswordRequest map(UserResetPasswordRequestDTO userResetPasswordRequestDTO);
}
