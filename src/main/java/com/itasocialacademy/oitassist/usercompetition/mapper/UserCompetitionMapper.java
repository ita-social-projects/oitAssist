package com.itasocialacademy.oitassist.usercompetition.mapper;

import com.itasocialacademy.oitassist.core.rest.mapper.GeneralMapper;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.CreateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.UpdateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.response.ResponseUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserCompetitionMapper
        extends GeneralMapper<UserCompetition, CreateUserCompetitionDTO, UpdateUserCompetitionDTO, ResponseUserCompetitionDTO> {

    @Override
    @Mapping(target = "competitionId", ignore = true)
    @Mapping(target = "id", ignore = true)
    UserCompetition toEntity(CreateUserCompetitionDTO dto);

    @Override
    @Mapping(target = "authorId", source = "id.authorId")
    @Mapping(target = "competitionId", source = "competitionId.id")
    ResponseUserCompetitionDTO toDto(UserCompetition entity);
}