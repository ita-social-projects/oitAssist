package com.itasocialacademy.oitassist.submission.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.submission.dao.dto.request.SubmissionCreateRequest;
import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.dao.repository.SubmissionRepository;
import com.itasocialacademy.oitassist.submission.exceptions.SubmissionNotFoundException;
import com.itasocialacademy.oitassist.submission.mapper.SubmissionMapper;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionServiceImpl implements SubmissionService {
    private final SubmissionRepository repository;
    private final SubmissionMapper submissionMapper;
    private final SecurityFacade securityFacade;
    private final FileManagerFacade fileManagerFacade;

    @Override
    @Transactional
    public SubmissionResponseDTO createSubmission(SubmissionCreateRequest submissionCreateRequest) {
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmissionBySubmittedByAndTaskAssignmentId(Long submittedBy,
                                                                               Long taskAssignmentId) {
        return submissionMapper.toResponse(
            repository.findBySubmittedByAndTaskAssignmentId(submittedBy, taskAssignmentId)
                .orElseThrow(() -> new SubmissionNotFoundException(submittedBy, taskAssignmentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getSubmissionById(Long id) {
        return submissionMapper.toResponse(
            repository.findById(id).orElseThrow(() -> new SubmissionNotFoundException(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionResponseDTO getMySubmissionByTaskAssignmentId(Long taskAssignmentId) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to view created tasks",
                ErrorCode.ACCESS_DENIED));
        return getSubmissionBySubmittedByAndTaskAssignmentId(currentUserId, taskAssignmentId);
    }
}
