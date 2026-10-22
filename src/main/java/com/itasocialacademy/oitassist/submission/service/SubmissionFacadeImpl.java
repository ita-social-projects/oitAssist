package com.itasocialacademy.oitassist.submission.service;

import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import java.util.List;
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
}
