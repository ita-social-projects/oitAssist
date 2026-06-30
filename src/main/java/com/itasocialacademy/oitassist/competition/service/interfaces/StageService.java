package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import java.util.List;

public interface StageService {
    StageResponse create(Long competitionId, CreateStageRequest request);

    StageResponse getById(Long id);
    List<StageResponse> getAllByCompetitionId(Long competitionId);
    StageResponse update(Long compId, Long stageId, UpdateStageRequest request);
    void delete(Long compId, Long stageId);

}
