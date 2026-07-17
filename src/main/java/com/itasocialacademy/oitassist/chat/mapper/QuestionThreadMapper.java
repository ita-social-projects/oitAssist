package com.itasocialacademy.oitassist.chat.mapper;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dto.response.QuestionThreadResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionThreadMapper {
    QuestionThreadResponseDTO toResponse(QuestionThread questionThread);
}