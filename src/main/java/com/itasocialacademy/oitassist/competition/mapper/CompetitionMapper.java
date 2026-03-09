package com.itasocialacademy.oitassist.competition.mapper;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateCompetitionDto;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponseDto;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CompetitionMapper extends GeneralMapper<Competition, CreateCompetitionDto, UpdateCompetitionDto, CompetitionResponseDto> {
    @Override
    CompetitionResponseDto toDTO(Competition competition);

    @Override
    Competition toEntity(CreateCompetitionDto d);

    @Override
    void merge(UpdateCompetitionDto d, @MappingTarget Competition competition);
}
