package com.itasocialacademy.oitassist.export.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;
import com.itasocialacademy.oitassist.export.exceptions.UnsupportedExportFormatException;
import com.itasocialacademy.oitassist.export.service.ExporterResolver;
import com.itasocialacademy.oitassist.export.service.interfaces.ExportService;
import com.itasocialacademy.oitassist.export.service.interfaces.Exporter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportControllerTest extends ControllerUnitTest<ExportController> {
    private static final String EXPORT_URL = "/api/v1/export";

    @Mock
    private ExportService exportService;

    @Mock
    private ExporterResolver exporterResolver;

    @Mock
    private Exporter exporter;

    @InjectMocks
    private ExportController exportController;

    @Override
    protected ExportController getController() {
        return exportController;
    }

    @Test
    void export_ShouldReturnOk_WhenFormatIsValid() throws Exception {
        ExportData data = new ExportData("Олімпіада", List.of());
        when(exportService.getExportData(any(), any(), any(), any())).thenReturn(data);
        when(exporterResolver.resolve(ExportFormat.EXCEL)).thenReturn(exporter);
        when(exporter.export(data)).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(EXPORT_URL)
            .param("olympiadId", "1")
            .param("format", "EXCEL"))
            .andExpect(status().isOk());
    }

    @Test
    void export_ShouldReturnBadRequest_WhenExportFormatIsUnsupported() throws Exception {
        ExportData data = new ExportData("Олімпіада", List.of());
        when(exportService.getExportData(any(), any(), any(), any())).thenReturn(data);
        when(exporterResolver.resolve(any()))
            .thenThrow(new UnsupportedExportFormatException("Unsupported export format: EXCEL"));

        mockMvc.perform(get(EXPORT_URL)
            .param("olympiadId", "1")
            .param("format", "EXCEL"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void export_ShouldPassSearchToService_WhenSearchProvided() throws Exception {
        ExportData data = new ExportData("Олімпіада", List.of());
        when(exportService.getExportData(any(), any(), any(), any())).thenReturn(data);
        when(exporterResolver.resolve(ExportFormat.EXCEL)).thenReturn(exporter);
        when(exporter.export(data)).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get(EXPORT_URL)
            .param("olympiadId", "1")
            .param("search", "Ігор")
            .param("format", "EXCEL"))
            .andExpect(status().isOk());

        verify(exportService).getExportData(eq(1L), any(), any(), eq("Ігор"));
    }
}
