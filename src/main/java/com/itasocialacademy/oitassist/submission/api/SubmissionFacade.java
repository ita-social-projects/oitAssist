package com.itasocialacademy.oitassist.submission.api;

import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;

public interface SubmissionFacade {
    SubmissionDetail getSubmissionById(Long id);
}
