package com.itasocialacademy.oitassist.competition.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "DTO representing list of tasks response related to certain competition")
public class ResponseCompetitionTasksDto {
    private Long id;
    private String title;
    private String description;
    private Long competitionId;
}
