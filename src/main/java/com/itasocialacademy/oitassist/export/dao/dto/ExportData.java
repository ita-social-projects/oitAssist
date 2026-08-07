package com.itasocialacademy.oitassist.export.dao.dto;

import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import java.util.List;

public record ExportData(
    String fileName,
    List<ParticipantResult> participants) {
}
