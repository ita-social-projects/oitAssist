package com.itasocialacademy.oitassist.taskassignment.service;

import com.itasocialacademy.oitassist.task.api.events.TaskDeletionRequestEvent;
import com.itasocialacademy.oitassist.task.exceptions.TaskDeletionRestrictedException;
import com.itasocialacademy.oitassist.taskassignment.service.interfaces.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskDeletionListener {
    private final AssignmentService assignmentService;

    @EventListener
    public void onTaskDeletionRequested(TaskDeletionRequestEvent deletionRequestEvent) {
        if (assignmentService.existsByTaskBodyId(deletionRequestEvent.taskBodyId())) {
            throw new TaskDeletionRestrictedException(deletionRequestEvent.taskBodyId());
        }
    }
}
