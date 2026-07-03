package com.itasocialacademy.oitassist.competition.dao.dto.validation;

import com.itasocialacademy.oitassist.competition.validation.DateRangeValidator;
import java.time.ZonedDateTime;

/**
 * Contract for DTOs that carry a start/end date range subject to validation.
 *
 * <p>
 * Implemented by request records (e.g. {@code CreateCompetitionRequest},
 * {@code CreateStageRequest}, {@code CreateTourRequest} and their corresponding
 * update variants) so that a single {@link DateRangeValidator} can validate the
 * {@code dateStart}/{@code dateFinish} pair across all of them without duplicating
 * validation logic per DTO.
 * </p>
 *
 * @see ValidDateRange
 * @see DateRangeValidator
 */
public interface HasDateRange {
    ZonedDateTime dateStart();
    ZonedDateTime dateFinish();
}