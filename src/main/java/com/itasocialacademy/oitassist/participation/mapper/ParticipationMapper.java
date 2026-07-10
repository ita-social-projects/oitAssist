package com.itasocialacademy.oitassist.participation.mapper;

import com.itasocialacademy.oitassist.participation.dao.model.Application;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import org.springframework.stereotype.Component;

@Component
public class ParticipationMapper {
    public Participation toParticipation(Application application) {
        Participation participation = new Participation();
        participation.setCompetitionId(application.getCompetitionId());
        participation.setStageId(application.getStageId());
        participation.setUserId(application.getIssuedBy());
        return participation;
    }
}
