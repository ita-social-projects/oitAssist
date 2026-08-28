package com.itasocialacademy.oitassist.participation.saver;

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
    public Invitation saveSingleInvitation(Long studentId, Long competitionId, Long stageId) {
        Invitation invitation = Invitation.builder()
            .studentId(studentId)
            .competitionId(competitionId)
            .stageId(stageId)
            .status(RequestStatus.PENDING)
            .build();

        return repository.save(invitation);
    }
}
