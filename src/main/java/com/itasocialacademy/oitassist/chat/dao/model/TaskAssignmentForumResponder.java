package com.itasocialacademy.oitassist.chat.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "task_assignment_forum_responders",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ta_forum_responders_assignment_responder",
        columnNames = {"task_assignment_id", "responder_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentForumResponder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_assignment_id", nullable = false)
    private Long taskAssignmentId;

    @Column(name = "responder_user_id", nullable = false)
    private Long responderUserId;

    @CreatedBy
    @Column(name = "assigned_by_user_id", nullable = false, updatable = false)
    private Long assignedByUserId;

    @CreatedDate
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}