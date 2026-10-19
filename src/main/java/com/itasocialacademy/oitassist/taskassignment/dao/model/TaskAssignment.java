package com.itasocialacademy.oitassist.taskassignment.dao.model;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "task_assignments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_task_assignments_task_tour",
        columnNames = {"task_body_id", "tour_id"}))
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_body_id", nullable = false)
    private Long taskBodyId;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private AssignmentVisibility visibility = AssignmentVisibility.HIDDEN;

    @Column(name = "max_points", nullable = false)
    @Positive
    private Integer maxPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "requirements", nullable = false, columnDefinition = "jsonb")
    private TaskRequirements requirements;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
