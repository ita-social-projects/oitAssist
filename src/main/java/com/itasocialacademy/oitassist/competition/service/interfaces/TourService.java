package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;

public interface TourService {
    TourResponse create(Long stageId, CreateTourRequest request);
}
