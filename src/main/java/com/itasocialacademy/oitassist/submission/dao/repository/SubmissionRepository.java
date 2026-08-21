package com.itasocialacademy.oitassist.submission.dao.repository;

import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findBySubmittedByAndTaskAssignmentId(Long submittedBy, Long taskAssignmentId);
}
