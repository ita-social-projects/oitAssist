package com.itasocialacademy.oitassist.submission.service;

import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionFacadeImpl implements SubmissionFacade {
    private final SubmissionService submissionService;
    private final SubmissionRepository submissionRepository;
    private final SubmissionMapper submissionMapper;

    @Override
    public SubmissionDetail getSubmissionById(Long id) {
        return submissionService.getSubmissionDetailById(id);
    }

    @Override
    public List<SubmissionDetail> getSubmissionsByTaskAssignmentId(Long taskAssignmentId) {
        return submissionRepository.findAllByTaskAssignmentId(taskAssignmentId)
            .stream()
            .map(submissionMapper::toDetail)
            .toList();
    }

    @Override
    public List<SubmissionDetail> getUserSubmissionsByTaskAssignmentIds(Long userId,
        Collection<Long> taskAssignmentIds) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(taskAssignmentIds, "taskAssignmentIds must not be null");
        if (taskAssignmentIds.isEmpty()) {
            return List.of();
        }
        return submissionRepository
            .findAllBySubmittedByAndTaskAssignmentIdIn(userId, taskAssignmentIds)
            .stream()
            .map(submissionMapper::toDetail)
            .toList();
    }
}