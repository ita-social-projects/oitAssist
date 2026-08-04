package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;

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

    boolean existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
        Long taskAssignmentId,
        Long assignedReviewerId,
        QuestionState state);

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.assignedReviewerId = :administratorId,
            question.status = IN_REVIEW,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.version = :expectedVersion
          AND question.state = OPEN
          AND question.status = NEW
          AND question.assignedReviewerId IS NULL
        """)
    int claimForReview(
        @Param("questionId") Long questionId,
        @Param("administratorId") Long administratorId,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    /**
     * Loads a question while acquiring a database write lock.
     *
     * <p>
     * The lock serializes official-answer publication with concurrent lifecycle
     * operations affecting the same question.
     * </p>
     *
     * @param questionId question identifier
     * @return locked question when it exists
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT question
        FROM QuestionThread question
        WHERE question.id = :questionId
        """)
    Optional<QuestionThread> findByIdForUpdate(
        @Param("questionId") Long questionId);

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.visibility = :visibility,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.version = :expectedVersion
        """)
    int updateVisibilityIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("visibility") QuestionVisibility visibility,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.status = :status,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.version = :expectedVersion
        """)
    int updateStatusIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("status") QuestionStatus status,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.state = :state,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.version = :expectedVersion
        """)
    int updateStateIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("state") QuestionState state,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);
}