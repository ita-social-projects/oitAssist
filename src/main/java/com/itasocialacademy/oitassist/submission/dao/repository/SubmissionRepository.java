package com.itasocialacademy.oitassist.submission.dao.repository;

import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
}
