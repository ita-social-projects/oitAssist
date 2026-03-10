package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFiltersDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionTasksDto;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompetitionService extends BaseService<Long, CreateCompetitionDto, UpdateCompetitionDto, ResponseCompetitionDto> {
    Page<ResponseCompetitionDto> getAllCompetitions(CompetitionFilter filter, Pageable pageable);

    CompetitionFiltersDto getFilters();

    List<ResponseCompetitionTasksDto> getAllCompetitionTasks(Long id);
}
