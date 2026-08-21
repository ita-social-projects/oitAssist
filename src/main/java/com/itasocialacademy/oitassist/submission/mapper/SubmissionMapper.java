package com.itasocialacademy.oitassist.submission.mapper;

import com.itasocialacademy.oitassist.submission.dao.dto.response.SubmissionResponseDTO;
import com.itasocialacademy.oitassist.submission.dao.model.Submission;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SubmissionMapper {
    SubmissionResponseDTO toResponse(Submission submission);
}
