package com.itasocialacademy.oitassist.task.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.model.Task;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing Task entities. Provides methods to retrieve tasks
 * related to competitions.
 */
@Repository
public interface TaskRepository extends EntityRepository<Task, Long> {
    /**
     * Retrieves all tasks for a specific competition as DTOs.
     *
     * @param competitionId the competition identifier
     * @return list of ResponseTaskDTO for the given competition
     */
    @Query("""
        SELECT new com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO(
            t.id,
            t.title,
            t.fileUrl,
            t.description,
            t.competitionId
            )
        FROM Task t
        WHERE t.competitionId = :competitionId
        """)
    List<ResponseTaskDTO> getTaskByCompetitionId(@Param("competitionId") Long competitionId);
}
