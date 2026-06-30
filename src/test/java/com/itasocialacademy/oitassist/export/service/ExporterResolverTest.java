package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;
import com.itasocialacademy.oitassist.export.exceptions.UnsupportedExportFormatException;
import com.itasocialacademy.oitassist.export.service.interfaces.Exporter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExporterResolverTest {
    @Mock
    private Exporter excelExporter;

    @Test
    void resolve_ShouldReturnExporter_WhenFormatIsSupported() {
        when(excelExporter.getFormat()).thenReturn(ExportFormat.EXCEL);
        ExporterResolver resolver = new ExporterResolver(List.of(excelExporter));

        Exporter result = resolver.resolve(ExportFormat.EXCEL);

        assertEquals(excelExporter, result);
    }

    @Test
    void resolve_ShouldThrowUnsupportedExportFormatException_WhenFormatHasNoExporter() {
        ExporterResolver resolver = new ExporterResolver(List.of());

        assertThrows(UnsupportedExportFormatException.class,
            () -> resolver.resolve(ExportFormat.EXCEL));
    }
}
