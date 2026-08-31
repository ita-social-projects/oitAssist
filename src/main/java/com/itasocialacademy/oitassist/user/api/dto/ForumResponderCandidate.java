package com.itasocialacademy.oitassist.user.api.dto;

import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import org.springframework.modulith.NamedInterface;

/**
 * Safe cross-module user projection used to validate and display a potential
 * TaskAssignment forum responder.
 *
 * <p>
 * The projection deliberately excludes passwords, activation tokens,
 * authentication credentials and persistence objects.
 * </p>
 */
@NamedInterface("ForumResponderCandidate")
public record ForumResponderCandidate(
    Long id,
    String email,
    String firstName,
    String lastName,
    Role role,
    UserStatus status) {
}