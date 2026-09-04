package com.itasocialacademy.oitassist.submission.dao.repository;

import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmittedByAndTaskAssignmentId(Long submittedBy, Long taskAssignmentId);

    @Modifying
    @Query(value = """
        INSERT INTO submissions (
            submitted_by,
            task_assignment_id,
            comment,
            submitted_at
        )
        VALUES (
            :userId,
            :taskAssignmentId,
            :comment,
            :submittedAt
        )
        ON CONFLICT (submitted_by, task_assignment_id)
        DO UPDATE SET
            comment = EXCLUDED.comment,
            submitted_at = EXCLUDED.submitted_at
        """, nativeQuery = true)
    void upsertSubmission(
        Long userId,
        Long taskAssignmentId,
        String comment,
        Instant submittedAt);
}
