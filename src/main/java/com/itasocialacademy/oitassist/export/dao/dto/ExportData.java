package com.itasocialacademy.oitassist.export.dao.dto;

import java.util.List;

public record ExportData(
    String olympiadName,
    List<ParticipantResult> participants) {
}
