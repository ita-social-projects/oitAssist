package com.itasocialacademy.oitassist.chat.mapper;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderDTO;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskAssignmentForumResponderMapper {
    TaskAssignmentForumResponderDTO toDto(TaskAssignmentForumResponder responder);
}