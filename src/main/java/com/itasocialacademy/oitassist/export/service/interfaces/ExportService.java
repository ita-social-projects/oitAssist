package com.itasocialacademy.oitassist.export.service.interfaces;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import java.util.List;

public interface ExportService {
    /**
     * Collects aggregated olympiad results for export. Uses the same scope and
     * search filter as the results page, so the exported file matches what is shown
     * on screen.
     *
     * @param olympiadId Competition ID
     * @param stageIds   stages to include, {@code null} or empty for no stage
     *                   filter
     * @param tourIds    tours to include, {@code null} or empty for no tour filter
     * @param search     optional participant's full name, case-insensitive, matches
     *                   any part of the name
     * @return {@link ExportData} with the generated file name and aggregated
     *         participants
     */
    ExportData getExportData(Long olympiadId, List<Long> stageIds, List<Long> tourIds, String search);
}
