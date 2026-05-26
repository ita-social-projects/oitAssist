package com.itasocialacademy.oitassist.user.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.model.ProfileUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.Instant;

@Repository
public interface ProfileUpdateRequestRepository extends EntityRepository<ProfileUpdateRequest, Long> {
    /** Check if entity with user id and status exists. */
    boolean existsByUserIdAndStatus(Long userId, UpdateRequestStatus status);

    /**
     * Checks if a profile update request exists for the given user within the
     * specified time range.
     */
    boolean existsByUserIdAndRequestedAtBetween(Long userId, Instant start, Instant end);

    /**
     * Returns a paginated list of profile update requests filtered by the given status.
     */
    Page<ProfileUpdateRequest> findByStatus(UpdateRequestStatus status, Pageable pageable);
}