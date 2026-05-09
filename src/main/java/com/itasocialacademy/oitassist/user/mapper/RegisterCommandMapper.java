package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.user.api.dto.RegisterCommand;
import com.itasocialacademy.oitassist.user.dao.model.User;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper that converts a {@link RegisterCommand} into a new
 * {@link User} entity ready for persistence.
 *
 * <p>
 * Replaces {@code auth.mapper.RegisterRequestMapper}, which was deleted as part
 * of moving registration logic into the {@code user} module. The key
 * difference: this mapper accepts the module-boundary DTO
 * ({@link RegisterCommand}) rather than the HTTP-layer DTO
 * ({@code RegisterRequest}), eliminating the cross-module type dependency.
 * </p>
 *
 * <p>
 * Fixed values applied on every registration:
 * </p>
 * <ul>
 * <li>{@code role} — always {@code USER}; role elevation is a separate,
 * admin-only operation.</li>
 * <li>{@code userStatus} — always {@code PENDING}; transitions to
 * {@code ACTIVE} after email verification.</li>
 * <li>{@code createdAt} — set to {@code Instant.now()} at mapping time.</li>
 * </ul>
 *
 * <p>
 * The {@code password} field is intentionally mapped as-is from the command.
 * The caller
 * ({@link com.itasocialacademy.oitassist.user.service.RegistrationServiceImpl})
 * is responsible for encoding the password before calling this mapper. The raw
 * password never reaches the database.
 * </p>
 */
@Mapper(componentModel = "spring", imports = {Instant.class})
public interface RegisterCommandMapper {
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "userStatus", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "userActivationToken", ignore = true)
    @Mapping(target = "id", ignore = true)
    User toEntity(RegisterCommand command);
}
