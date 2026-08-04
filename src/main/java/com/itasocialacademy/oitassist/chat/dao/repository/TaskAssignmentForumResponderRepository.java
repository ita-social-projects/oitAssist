package com.itasocialacademy.oitassist.chat.dao.repository;

import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentForumResponderRepository
    extends JpaRepository<TaskAssignmentForumResponder, Long> {
    Optional<TaskAssignmentForumResponder> findByTaskAssignmentIdAndResponderUserId(
        Long taskAssignmentId,
        Long responderUserId);

    boolean existsByTaskAssignmentIdAndResponderUserId(
        Long taskAssignmentId,
        Long responderUserId);

    Page<TaskAssignmentForumResponder> findAllByTaskAssignmentId(
        Long taskAssignmentId,
        Pageable pageable);

    List<TaskAssignmentForumResponder> findAllByResponderUserId(
        Long responderUserId);

    long deleteByTaskAssignmentIdAndResponderUserId(
        Long taskAssignmentId,
        Long responderUserId);

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
}