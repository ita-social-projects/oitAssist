package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;

public interface CompetitionService extends BaseService<Long, CreateCompetitionDto, UpdateCompetitionDto, ResponseCompetitionDto> {
}
