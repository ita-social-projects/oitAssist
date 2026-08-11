package com.itasocialacademy.oitassist.evaluation.dao.dto.request;

public record ParticipantTaskScore(Long userId,
    String participantName,
    Long tourId,
    Long taskId,
    Integer score) {
}
