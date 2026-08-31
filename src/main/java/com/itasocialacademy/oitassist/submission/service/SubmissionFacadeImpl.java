package com.itasocialacademy.oitassist.submission.service;

import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.submission.service.interfaces.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionFacadeImpl implements SubmissionFacade {
    private final SubmissionService submissionService;

    @Override
    public SubmissionDetail getSubmissionById(Long id) {
        return submissionService.getSubmissionDetailById(id);
    }
}
