package com.itasocialacademy.oitassist.chat.mapper;

import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.user.api.dto.ForumResponderCandidate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TaskAssignmentForumResponderMapper {
    @Mapping(target = "id", source = "assignment.id")
    @Mapping(target = "taskAssignmentId", source = "assignment.taskAssignmentId")
    @Mapping(target = "responderUserId", source = "assignment.responderUserId")
    @Mapping(target = "responderEmail", source = "candidate.email")
    @Mapping(target = "responderFirstName", source = "candidate.firstName")
    @Mapping(target = "responderLastName", source = "candidate.lastName")
    @Mapping(target = "assignedByUserId", source = "assignment.assignedByUserId")
    @Mapping(target = "assignedAt", source = "assignment.assignedAt")
    TaskAssignmentForumResponderResponseDTO toResponse(
        TaskAssignmentForumResponder assignment,
        ForumResponderCandidate candidate);
}