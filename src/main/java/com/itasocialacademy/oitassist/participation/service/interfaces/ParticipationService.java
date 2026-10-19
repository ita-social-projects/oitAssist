package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationListItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ParticipationService {
    Page<ParticipationListItemResponse> getParticipationList(
        Long competitionId,
        Long stageId,
        String search,
        Pageable pageable);

    boolean competitionHasParticipants(Long competitionId);

    boolean stageHasParticipants(Long stageId);

    boolean isUserParticipant(Long userId, Long competitionId, Long stageId);

    boolean isUserAStageParticipant(Long userId, Long stageId);
}
