package org.tkit.onecx.user.profile.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.org.tkit.onecx.image.bff.clients.model.ImageInfo;
import gen.org.tkit.onecx.image.bff.clients.model.RefType;
import gen.org.tkit.onecx.user.profile.bff.clients.model.*;
import gen.org.tkit.onecx.user.profile.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface UserProfileMapper {

    UserProfileDTO map(UserProfile userProfile);

    UserPersonCriteria map(UserPersonCriteriaDTO userPersonCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    UserProfilePageResultDTO map(UserProfilePageResult userProfilePageResult);

    UpdateUserProfileRequest map(UpdateUserProfileRequestDTO updateUserPersonRequestDTO);

    UserPersonDTO map(UserPerson userPerson);

    @Mapping(target = "modificationCount", ignore = true)
    UserProfileAccountSettingsDTO map(UserProfileAccountSettings userProfileAccountSettings);

    RefType map(RefTypeDTO imageInfo);

    ImageInfoDTO map(ImageInfo imageInfo);

    UpdateUserProfileRequest mapUpdate(UpdateUserProfileRequestDTO updateUserProfileRequestDTO);
}
