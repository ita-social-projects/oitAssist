package com.itasocialacademy.oitassist.task.dao.model;

import com.itasocialacademy.oitassist.task.dao.model.id.TaskOwnerId;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Table(name = "task_owners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskOwner {
    @EmbeddedId
    private TaskOwnerId id;

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private TaskBody task;

    @CreatedDate
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
}