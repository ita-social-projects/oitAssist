package com.itasocialacademy.oitassist.participation.mapper;

import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.model.ParticipationRequestEvent;
import org.springframework.stereotype.Component;

@Component
public class ParticipationMapper {
    public Participation toParticipation(ParticipationRequestEvent event) {
        if (event == null) {
            return null;
        }
        Participation participation = new Participation();
        participation.setCompetitionId(event.getCompetitionId());
        participation.setStageId(event.getStageId());
        participation.setUserId(event.getUserId());
        return participation;
    }
}
