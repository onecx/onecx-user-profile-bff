package io.github.onecx.user.profile.bff.rs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

import gen.io.github.onecx.user.profile.bff.clients.model.*;
import gen.io.github.onecx.user.profile.bff.rs.internal.model.*;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface UserProfileMapper {

    UserProfileDTO map(UserProfile userProfile);

    UserPersonCriteria map(UserPersonCriteriaDTO userPersonCriteriaDTO);

    @Mapping(target = "removeStreamItem", ignore = true)
    UserProfilePageResultDTO map(UserProfilePageResult userProfilePageResult);

    UpdateUserPersonRequest map(UpdateUserPersonRequestDTO updateUserPersonRequestDTO);

    CreateUserPreference map(CreateUserPreferenceDTO createUserPreferenceDTO);

    UserPreferenceDTO map(UserPreference preferenceResponse);

    UserPersonDTO map(UserPerson userPerson);

    @Mapping(target = "removePreferencesItem", ignore = true)
    UserPreferencesDTO map(UserPreferences userPreferences);

    UserProfileAccountSettingsDTO map(UserProfileAccountSettings userProfileAccountSettings);

    UpdateUserPersonRequest map(UpdateUserPersonDTO updateUserPersonDTO);

    UpdateUserSettings map(UpdateUserSettingsDTO updateUserSettingsDTO);

    ImageInfoDTO map(ImageInfo imageInfo);
}
