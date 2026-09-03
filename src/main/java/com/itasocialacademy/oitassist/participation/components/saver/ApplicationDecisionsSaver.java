package com.itasocialacademy.oitassist.participation.components.saver;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ApplicationRepository;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ApplicationDecisionsSaver {
    private final ApplicationRepository applicationRepository;
    private final ParticipationRepository participationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Participation saveAcceptedApplicationData(
        Long organizerId,
        Application application,
        Long competitionId,
        Long stageId) {
        application.setProcessedBy(organizerId);
        application.setProcessedAt(Instant.now());
        application.setStatus(RequestStatus.ACCEPTED);
        applicationRepository.save(application);

        Participation participation = Participation.builder()
            .userId(application.getUserId())
            .competitionId(competitionId)
            .stageId(stageId)
            .build();
        return participationRepository.save(participation);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Application saveRejectedApplication(Long organizerId, Application application, String rejectionReason) {
        application.setProcessedBy(organizerId);
        application.setProcessedAt(Instant.now());
        application.setRejectionReason(rejectionReason);
        application.setStatus(RequestStatus.REJECTED);

        return applicationRepository.save(application);
    }
}

