package com.itasocialacademy.oitassist.task.api.interfaces;

import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import java.util.List;
import org.springframework.modulith.NamedInterface;

/**
 * Facade interface for retrieving tasks from other spring module. Provides
 * methods for fetching tasks by competition ID.
 */
@NamedInterface("TaskFacade")
public interface TaskFacade {
    /**
     * Retrieves all tasks associated with a specific competition.
     *
     * @param id competition identifier
     * @return list of task DTOs
     */
    public List<ResponseTaskDTO> getTasksByCompetitionId(Long id);
}
