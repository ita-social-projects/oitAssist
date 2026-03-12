package com.itasocialacademy.oitassist.task.api.interfaces;

import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("TaskFacade")
public interface TaskFacade {
    public List<ResponseTaskDTO> getTasksByCompetitionId(Long id);
}
