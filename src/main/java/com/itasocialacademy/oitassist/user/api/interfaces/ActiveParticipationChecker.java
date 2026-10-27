package com.itasocialacademy.oitassist.user.api.interfaces;

import org.springframework.modulith.NamedInterface;

@NamedInterface("ActiveParticipationChecker")
public interface ActiveParticipationChecker {
    boolean hasActiveParticipation(Long userId);
}