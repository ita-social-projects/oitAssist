package com.itasocialacademy.oitassist.task.dao.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TaskOwnerId implements Serializable {
    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "owner_id")
    private Long ownerId;
}