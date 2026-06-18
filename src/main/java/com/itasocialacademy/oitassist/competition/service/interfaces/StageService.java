package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;

public interface StageService {
    StageResponse create(Long competitionId, CreateStageRequest request);

    StageResponse getById(Long id);
}
