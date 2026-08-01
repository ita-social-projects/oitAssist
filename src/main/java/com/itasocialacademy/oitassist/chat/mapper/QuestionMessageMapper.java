package com.itasocialacademy.oitassist.chat.mapper;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionMessageMapper {
    QuestionMessageResponseDTO toResponse(
            QuestionMessage questionMessage);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "content", source = "content")
    QuestionMessage toEntity(
            CreateCommentRequestDTO request);
}