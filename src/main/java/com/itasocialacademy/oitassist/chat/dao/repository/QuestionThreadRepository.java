package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionThreadRepository extends JpaRepository<QuestionThread, Long> {
    @Query("""
        SELECT question
        FROM QuestionThread question
        WHERE question.taskAssignmentId = :taskAssignmentId
          AND (
              question.visibility = PUBLIC
              OR (
                  question.visibility = PRIVATE
                  AND question.authorId = :participantId
              )
          )
        """)
    Page<QuestionThread> findParticipantVisibleQuestions(
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("participantId") Long participantId,
        Pageable pageable);

    Page<QuestionThread> findAllByStateAndStatusAndAssignedReviewerIdIsNull(
        QuestionState state,
        QuestionStatus status,
        Pageable pageable);

    Page<QuestionThread> findAllByStateAndAssignedReviewerId(
        QuestionState state,
        Long assignedReviewerId,
        Pageable pageable);

    Page<QuestionThread> findAllByStateAndAssignedReviewerIdAndStatus(
        QuestionState state,
        Long assignedReviewerId,
        QuestionStatus status,
        Pageable pageable);
}