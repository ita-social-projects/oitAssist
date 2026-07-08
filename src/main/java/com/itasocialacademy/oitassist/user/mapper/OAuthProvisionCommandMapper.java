package com.itasocialacademy.oitassist.user.mapper;

import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.dao.model.User;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper that converts an {@link OAuthProvisionCommand} into a new
 * {@link User} entity ready for persistence.
 *
 * <p>
 * Distinct from {@link RegisterCommandMapper} because the constants applied at
 * mapping time differ:
 * </p>
 * <ul>
 * <li>{@code role} — always {@code USER}, same as password registration.</li>
 * <li>{@code userStatus} — always {@code ACTIVE}, because the provider has
 * already verified the email. Password registration uses {@code PENDING} and
 * waits for the activation link.</li>
 * <li>{@code createdAt} — set to {@code Instant.now()} at mapping time.</li>
 * <li>{@code surname} — defaults to empty string when the provider didn't
 * supply a {@code family_name} claim. The column is {@code NOT NULL} so empty
 * is the safest fallback that doesn't require a schema change.</li>
 * </ul>
 *
 * <p>
 * The {@code password} field is intentionally not mapped here. The caller
 * ({@code RegistrationServiceImpl.provisionOAuthUser}) is responsible for
 * generating an unguessable random password hash and setting it on the entity
 * before persistence. Keeping the random-generation logic in the service — not
 * the mapper — is deliberate: mappers should be pure transformations of their
 * inputs, not sources of new entropy.
 * </p>
 */
@Mapper(componentModel = "spring", imports = {Instant.class})
public interface OAuthProvisionCommandMapper {
    @Mapping(target = "role", constant = "USER")
    @Mapping(target = "userStatus", constant = "ACTIVE")
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "surname", source = "surname", defaultValue = "")
    @Mapping(target = "userActivationToken", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    User toEntity(OAuthProvisionCommand command);
}
