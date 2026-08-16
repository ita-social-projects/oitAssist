package com.itasocialacademy.oitassist.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import com.itasocialacademy.oitassist.user.dao.model.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class OAuthProvisionCommandMapperTest {
    private static final String EMAIL = "ivan@example.com";
    private static final String FIRST_NAME = "Ivan";
    private static final String SURNAME = "Petrenko";
    private static final String MIDDLE_NAME = "Mykolayovych";
    private static final String PHONE = "+380501234567";
    private static final String EMPTY_SURNAME_FALLBACK = "";

    private OAuthProvisionCommandMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(OAuthProvisionCommandMapper.class);
    }

    @Test
    void toEntity_ShouldCopyAllProvidedFields_WhenCommandIsFullyPopulated() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .surname(SURNAME)
            .middleName(MIDDLE_NAME)
            .phoneNumber(PHONE)
            .build();

        User entity = mapper.toEntity(command);

        assertThat(entity.getEmail()).isEqualTo(EMAIL);
        assertThat(entity.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(entity.getSurname()).isEqualTo(SURNAME);
        assertThat(entity.getMiddleName()).isEqualTo(MIDDLE_NAME);
        assertThat(entity.getPhoneNumber()).isEqualTo(PHONE);
    }

    @Test
    void toEntity_ShouldApplyConstantRoleAndStatus_WhenMappingAnyCommand() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User entity = mapper.toEntity(command);

        assertThat(entity.getRole()).isEqualTo(Role.USER);
        assertThat(entity.getUserStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void toEntity_ShouldSetCreatedAtToCurrentInstant_WhenMappingAnyCommand() {
        Instant before = Instant.now();
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User entity = mapper.toEntity(command);

        Instant after = Instant.now();
        assertThat(entity.getCreatedAt()).isBetween(before, after);
    }

    @Test
    void toEntity_ShouldSubstituteEmptySurname_WhenSurnameIsNull() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .surname(null)
            .build();

        User entity = mapper.toEntity(command);

        assertThat(entity.getSurname()).isEqualTo(EMPTY_SURNAME_FALLBACK);
    }

    @Test
    void toEntity_ShouldPreserveNullMiddleName_WhenMiddleNameIsNull() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .middleName(null)
            .build();

        User entity = mapper.toEntity(command);

        assertThat(entity.getMiddleName()).isNull();
    }

    @Test
    void toEntity_ShouldLeavePasswordAndIdAndTokenUnset_WhenMappingAnyCommand() {
        OAuthProvisionCommand command = OAuthProvisionCommand.builder()
            .email(EMAIL)
            .firstName(FIRST_NAME)
            .build();

        User entity = mapper.toEntity(command);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getUserActivationToken()).isNull();
    }
}