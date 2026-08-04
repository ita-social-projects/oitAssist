package com.itasocialacademy.oitassist.chat.mapper;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionThreadMapper {
    QuestionThreadResponseDTO toResponse(QuestionThread questionThread);

    QuestionThreadSummaryResponseDTO toSummaryResponse(QuestionThread questionThread);

    AdminQuestionInboxItemResponseDTO toAdminInboxItemResponse(QuestionThread questionThread);

    QuestionReviewInboxItemResponseDTO toReviewInboxItemResponse(QuestionThread questionThread);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "content", source = "content")
    QuestionThread toEntity(CreateQuestionRequestDTO request);
}