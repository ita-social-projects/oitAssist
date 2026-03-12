package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFiltersDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionSpecification;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.task.api.interfaces.TaskFacade;
import com.itasocialacademy.oitassist.task.dao.dto.response.ResponseTaskDTO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CompetitionServiceImpl
    extends
    AbstractServiceImpl<Long, Competition, CreateCompetitionDto, UpdateCompetitionDto, ResponseCompetitionDto, CompetitionRepository, CompetitionMapper>
    implements CompetitionService {
    private TaskFacade taskFacade;

    protected CompetitionServiceImpl(CompetitionRepository repository, CompetitionMapper mapper,
        TaskFacade taskFacade) {
        super(repository, mapper);
        this.taskFacade = taskFacade;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ResponseCompetitionDto> getAllCompetitions(CompetitionFilter filter, Pageable pageable) {
        Page<Competition> page = repository.findAll(CompetitionSpecification.filter(filter), pageable);
        return page.map(mapper::toDTO);
    }

    @Transactional(readOnly = true)
    @Override
    public CompetitionFiltersDto getFilters() {
        return CompetitionFiltersDto.builder()
            .levels(List.of(CompetitionLevel.values()))
            .years(repository.getYears())
            .build();
    }

    @Override
    public List<ResponseTaskDTO> getAllCompetitionTasks(Long id) {
        return taskFacade.getTasksByCompetitionId(id);
    }
}
