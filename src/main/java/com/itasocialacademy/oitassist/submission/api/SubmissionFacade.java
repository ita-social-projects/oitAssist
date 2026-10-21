package com.itasocialacademy.oitassist.submission.api;

import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import java.util.List;

public interface SubmissionFacade {
    SubmissionDetail getSubmissionById(Long id);

    List<SubmissionDetail> getSubmissionsByTaskAssignmentId(Long taskAssignmentId);
}
