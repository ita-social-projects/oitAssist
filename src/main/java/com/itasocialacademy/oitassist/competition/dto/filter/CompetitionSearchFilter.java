package com.itasocialacademy.oitassist.competition.dto.filter;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

@Builder
@ValidDateRange
public record CompetitionSearchFilter(
    String title,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    ZonedDateTime dateStart,

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    ZonedDateTime dateFinish,

    List<CompetitionStatus> statuses
) implements HasDateRange {
}