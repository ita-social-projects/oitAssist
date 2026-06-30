package com.itasocialacademy.oitassist.export.service.interfaces;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import java.util.List;

public interface ExportService {
    ExportData getExportData(Long olympiadId, List<Long> stageIds, List<Long> tourIds);
}
