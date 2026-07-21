package com.itasocialacademy.oitassist.task.api.facade;

import com.itasocialacademy.oitassist.task.api.dto.TaskForumContext;
import com.itasocialacademy.oitassist.task.api.interfaces.TaskForumFacade;
import com.itasocialacademy.oitassist.task.dao.repository.TaskBodyRepository;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.task.mapper.TaskBodyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class TaskForumFacadeImpl implements TaskForumFacade {
    private final TaskBodyRepository taskBodyRepository;
    private final TaskBodyMapper taskBodyMapper;

    @Override
    @Transactional(readOnly = true)
    public TaskForumContext getForumContext(Long taskId) {
        return taskBodyRepository.findById(taskId)
            .map(taskBodyMapper::toForumContext)
            .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserAccessForum(Long taskId, Long userId) {
        return taskId != null
            && userId != null
            && taskBodyRepository.existsById(taskId);
    }
}