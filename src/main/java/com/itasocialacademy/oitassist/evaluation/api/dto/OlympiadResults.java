package com.itasocialacademy.oitassist.evaluation.api.dto;

import java.util.List;

public record OlympiadResults(
    String olympiadTitle,
    String scopeTitle,
    List<ParticipantResult> participants) {
}
