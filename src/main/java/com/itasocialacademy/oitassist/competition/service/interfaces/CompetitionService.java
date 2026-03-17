package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFiltersDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing competitions. Provides methods for CRUD
 * operations, retrieving filtered and paginated competitions, fetching
 * competition filters, and getting tasks related to competitions.
 */
public interface CompetitionService
    extends BaseService<Long, CreateCompetitionDto, UpdateCompetitionDto, ResponseCompetitionDto> {

    /**
     * Retrieves a paginated list of competitions with optional filtering.
     *
     * @param filter   filtering criteria
     * @param pageable pagination information
     * @return paginated list of competitions
     */
    Page<ResponseCompetitionDto> getAllCompetitions(CompetitionFilter filter, Pageable pageable);

    /**
     * Returns available filter values for competitions.
     *
     * @return DTO containing filter options such as level, years
     */
    CompetitionFiltersDto getFilters();

    /**
     * Retrieves all tasks that belong to a specific competition.
     *
     * @param id competition identifier
     * @return list of tasks for the given competition
     */
    List<ResponseTaskDTO> getAllCompetitionTasks(Long id);
}
