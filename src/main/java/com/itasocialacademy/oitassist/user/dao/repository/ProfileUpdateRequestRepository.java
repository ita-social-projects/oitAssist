package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public interface ProfileUpdateRequestRepository {
    /** Check if entity with user id and status exists. */
    boolean existsByUserIdAndStatus(Long userId, UpdateRequestStatus status);

    /**
     * Checks if a profile update request exists for the given user within the
     * specified time range.
     */
    boolean existsByUserIdAndRequestedAtBetween(Long userId, Instant start, Instant end);

    /** Persists the given profile update request. */
    ProfileUpdateRequest save(ProfileUpdateRequest request);
}