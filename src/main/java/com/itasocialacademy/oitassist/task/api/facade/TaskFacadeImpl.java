package com.itasocialacademy.oitassist.task.api.facade;

import com.itasocialacademy.oitassist.task.api.interfaces.TaskFacade;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import com.itasocialacademy.oitassist.task.dao.repository.TaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskFacadeImpl implements TaskFacade {
    private final TaskRepository repository;

    @Override
    public List<ResponseTaskDTO> getTasksByCompetitionId(Long id) {
        return repository.getTaskByCompetitionId(id);
    }
}
