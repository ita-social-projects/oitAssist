package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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
    Optional<QuestionThread> findByIdForUpdate(@Param("questionId") Long questionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    @Query("""
        SELECT question
        FROM QuestionThread question
        WHERE question.taskAssignmentId = :taskAssignmentId
        """)
    Page<QuestionThread> findAllQuestionsByTaskAssignmentId(
        @Param("taskAssignmentId") Long taskAssignmentId,
        Pageable pageable);

    Page<QuestionThread> findAllByStateAndStatusAndAssignedReviewerIdIsNull(
        QuestionState state,
        QuestionStatus status,
        Pageable pageable);

    /**
     * Returns unclaimed questions visible to one exact TaskAssignment forum
     * responder.
     *
     * <p>
     * Responder eligibility is evaluated in the database through the chat-owned
     * {@link TaskAssignmentForumResponder} entity. Visibility is deliberately not
     * part of the query because eligible responders may review both private and
     * public questions.
     * </p>
     */
    @Query("""
        SELECT question
        FROM QuestionThread question
        WHERE question.state = :state
          AND question.status = :status
          AND question.assignedReviewerId IS NULL
          AND EXISTS (
              SELECT responder.id
              FROM TaskAssignmentForumResponder responder
              WHERE responder.taskAssignmentId = question.taskAssignmentId
                AND responder.responderUserId = :responderUserId
          )
        """)
    Page<QuestionThread> findResponderUnclaimedQuestions(
        @Param("responderUserId") Long responderUserId,
        @Param("state") QuestionState state,
        @Param("status") QuestionStatus status,
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

    @Query("""
        SELECT question.taskAssignmentId
        FROM QuestionThread question
        WHERE question.id = :questionId
        """)
    Optional<Long> findTaskAssignmentIdByQuestionId(@Param("questionId") Long questionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.assignedReviewerId = :responderUserId,
            question.status = IN_REVIEW,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.taskAssignmentId = :taskAssignmentId
          AND question.version = :expectedVersion
          AND question.state = OPEN
          AND question.status = NEW
          AND question.assignedReviewerId IS NULL
        """)
    int claimForReviewAsResponder(
        @Param("questionId") Long questionId,
        @Param("responderUserId") Long responderUserId,
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.visibility = :visibility,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.taskAssignmentId = :taskAssignmentId
          AND question.version = :expectedVersion
        """)
    int updateVisibilityAsResponderIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("responderUserId") Long responderUserId,
        @Param("visibility") QuestionVisibility visibility,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.status = :status,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.taskAssignmentId = :taskAssignmentId
          AND question.version = :expectedVersion
        """)
    int updateStatusAsResponderIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("responderUserId") Long responderUserId,
        @Param("status") QuestionStatus status,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE QuestionThread question
        SET question.state = :state,
            question.updatedAt = :updatedAt,
            question.version = question.version + 1
        WHERE question.id = :questionId
          AND question.taskAssignmentId = :taskAssignmentId
          AND question.version = :expectedVersion
        """)
    int updateStateAsResponderIfVersionMatches(
        @Param("questionId") Long questionId,
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("responderUserId") Long responderUserId,
        @Param("state") QuestionState state,
        @Param("expectedVersion") Long expectedVersion,
        @Param("updatedAt") Instant updatedAt);
}