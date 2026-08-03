package com.itasocialacademy.oitassist.participation.saver;

import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import com.itasocialacademy.oitassist.participation.dao.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InvitationRequestsSaver {
    private final InvitationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Invitation saveSingleInvitation(Long studentId, CreateInvitationRequest request) {
        Invitation invitation = Invitation.builder()
            .studentId(studentId)
            .competitionId(request.getCompetitionId())
            .stageId(request.getStageId())
            .status(RequestStatus.PENDING)
            .build();

        return repository.save(invitation);
    }
}
