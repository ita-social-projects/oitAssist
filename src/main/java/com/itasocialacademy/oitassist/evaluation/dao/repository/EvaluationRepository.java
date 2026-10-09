package com.itasocialacademy.oitassist.evaluation.dao.repository;

import com.itasocialacademy.oitassist.evaluation.dao.model.Evaluation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findBySubmissionId(Long submissionId);
}
