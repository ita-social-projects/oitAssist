package com.itasocialacademy.oitassist.user.api.dto;

import org.springframework.modulith.NamedInterface;

/**
 * Display-side projection of a {@code User}, exposed across module boundaries
 * so consuming modules can retrieve the user's email and first name without
 * depending on the full {@code User} entity.
 */
@NamedInterface("UserProfileDetails")
public record UserProfileDetails(
    Long id,
    String firstName,
    String email) {
}
