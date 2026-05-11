package com.itasocialacademy.oitassist.security.api.mapper;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Maps cross-module DTOs from the {@code user} module into the
 * {@code security}-internal {@link UserDetailsImpl}.
 *
 * <p>
 * This mapper is the canonical place where the {@code user}-side {@link Role}
 * enum is wrapped into a Spring Security {@link SimpleGrantedAuthority}. The
 * {@code user} module deliberately exposes {@code Role} as a plain enum so it
 * stays free of Spring Security types; this mapper, on the {@code security}
 * side of the module boundary, applies the {@code ROLE_} prefix convention.
 * </p>
 *
 * <p>
 * Currently used by the OAuth2 social login success path: after
 * {@code UserFacade.provisionOAuthUser} returns a {@link UserAuthDetails}, the
 * success handler maps it through here to obtain the {@link UserDetailsImpl}
 * needed by {@code JwtTokenIssuer}. The password sign-in path stays on
 * {@code SecurityUserProviderImpl} which maps directly from the {@code User}
 * entity inside the {@code user} module.
 * </p>
 */
@Mapper(componentModel = "spring", imports = {SimpleGrantedAuthority.class, List.class})
public interface UserDetailsMapper {
    /**
     * Converts the cross-module auth projection into a fully populated
     * {@link UserDetailsImpl} carrying the encoded password and a single role
     * authority. The role is wrapped into the standard Spring Security
     * {@code ROLE_}-prefixed authority.
     *
     * @param authDetails the cross-module auth projection; must not be null
     * @return a {@link UserDetailsImpl} ready for use in the Spring Security
     *         context
     */
    @Mapping(
        target = "authorities",
        expression = "java(List.of(new SimpleGrantedAuthority(\"ROLE_\" + authDetails.role().name())))")
    UserDetailsImpl toUserDetails(UserAuthDetails authDetails);
}