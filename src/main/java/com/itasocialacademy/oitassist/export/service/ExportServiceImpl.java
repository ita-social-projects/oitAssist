package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.dto.ParticipantResult;
import com.itasocialacademy.oitassist.export.service.interfaces.ExportService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExportServiceImpl implements ExportService {
    @Override
    public ExportData getExportData(Long olympiadId, List<Long> stageIds, List<Long> tourIds) {
        ParticipantResult ihor = new ParticipantResult("Ігор", 45, List.of());
        ParticipantResult karina = new ParticipantResult("Каріна", 58, List.of());

        return new ExportData("Олімпіада з математики", List.of(ihor, karina));
    }
}
