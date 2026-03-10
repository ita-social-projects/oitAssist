package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.ResponseCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CompetitionServiceImpl
    extends AbstractServiceImpl<
        Long,
        Competition,
        CreateCompetitionDto,
        UpdateCompetitionDto,
    ResponseCompetitionDto,
        CompetitionRepository,
        CompetitionMapper
    >
    implements CompetitionService {

    protected CompetitionServiceImpl(CompetitionRepository repository, CompetitionMapper mapper) {
        super(repository, mapper);
    }
}
