package com.itasocialacademy.oitassist.chat.realtime;

import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OrganizationRealtimeRecipientResolver {
    private final TaskAssignmentForumResponderRepository responderRepository;
    private final UserFacade userFacade;

    @Transactional(readOnly = true)
    public List<Long> resolveInboxRecipients(Long taskAssignmentId) {
        requirePositiveId(taskAssignmentId, "Task assignment id");
        return responderRepository.findDistinctResponderUserIdsByTaskAssignmentId(taskAssignmentId).stream()
            .distinct()
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean isOrganizationResponder(Long taskAssignmentId, Long userId) {
        if (taskAssignmentId == null || taskAssignmentId <= 0 || userId == null || userId <= 0) {
            return false;
        }
        return responderRepository.existsByTaskAssignmentIdAndResponderUserId(taskAssignmentId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isEligibleOrganizationResponder(Long taskAssignmentId, Long userId) {
        return isOrganizationResponder(taskAssignmentId, userId)
            && userFacade.findForumResponderCandidateById(userId)
                .filter(candidate -> candidate.role() == Role.ORG && candidate.status() == UserStatus.ACTIVE)
                .isPresent();
    }

    private void requirePositiveId(Long identifier, String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new IllegalArgumentException("%s must be a positive number".formatted(fieldName));
        }
    }
}
