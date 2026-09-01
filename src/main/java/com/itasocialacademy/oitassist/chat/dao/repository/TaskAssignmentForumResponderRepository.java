package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
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
public interface TaskAssignmentForumResponderRepository extends JpaRepository<TaskAssignmentForumResponder, Long> {
    Optional<TaskAssignmentForumResponder> findByTaskAssignmentIdAndResponderUserId(
        Long taskAssignmentId,
        Long responderUserId);

    boolean existsByTaskAssignmentIdAndResponderUserId(Long taskAssignmentId, Long responderUserId);

    Page<TaskAssignmentForumResponder> findAllByTaskAssignmentId(Long taskAssignmentId, Pageable pageable);

    List<TaskAssignmentForumResponder> findAllByResponderUserId(Long responderUserId);

    long deleteByTaskAssignmentIdAndResponderUserId(Long taskAssignmentId, Long responderUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT responder
        FROM TaskAssignmentForumResponder responder
        WHERE responder.taskAssignmentId = :taskAssignmentId
          AND responder.responderUserId = :responderUserId
        """)
    Optional<TaskAssignmentForumResponder> findByTaskAssignmentIdAndResponderUserIdForUpdate(
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("responderUserId") Long responderUserId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = """
            INSERT INTO task_assignment_forum_responders (
                task_assignment_id,
                responder_user_id,
                assigned_by_user_id,
                assigned_at
            )
            VALUES (
                :taskAssignmentId,
                :responderUserId,
                :assignedByUserId,
                :assignedAt
            )
            ON CONFLICT (
                task_assignment_id,
                responder_user_id
            )
            DO NOTHING
            """,
        nativeQuery = true)
    int insertIfAbsent(
        @Param("taskAssignmentId") Long taskAssignmentId,
        @Param("responderUserId") Long responderUserId,
        @Param("assignedByUserId") Long assignedByUserId,
        @Param("assignedAt") Instant assignedAt);

    @Query("""
        SELECT responder.taskAssignmentId
        FROM TaskAssignmentForumResponder responder
        WHERE responder.responderUserId = :responderUserId
        ORDER BY responder.taskAssignmentId ASC
        """)
    List<Long> findTaskAssignmentIdsByResponderUserId(@Param("responderUserId") Long responderUserId);

    @Query("""
        SELECT DISTINCT responder.responderUserId
        FROM TaskAssignmentForumResponder responder
        WHERE responder.taskAssignmentId = :taskAssignmentId
        ORDER BY responder.responderUserId ASC
        """)
    List<Long> findDistinctResponderUserIdsByTaskAssignmentId(@Param("taskAssignmentId") Long taskAssignmentId);
}