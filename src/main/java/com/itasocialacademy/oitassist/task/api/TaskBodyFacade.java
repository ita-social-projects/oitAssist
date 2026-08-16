package com.itasocialacademy.oitassist.task.api;

import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade providing operations for managing task bodies from other modules.
 * Handles retrieval, querying and creation of task body entities.
 */
public interface TaskBodyFacade {
    Optional<TaskBodyDetail> findTaskBodyById(Long taskBodyId);

    Map<Long, String> getTaskTitlesByIds(List<Long> taskBodyIds);

    TaskBodyDetail createTask(String title, String description, List<Long> fileIds);
}
