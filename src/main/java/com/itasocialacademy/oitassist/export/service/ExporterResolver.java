package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;
import com.itasocialacademy.oitassist.export.exceptions.UnsupportedExportFormatException;
import com.itasocialacademy.oitassist.export.service.interfaces.Exporter;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ExporterResolver {
    private final List<Exporter> exporters;

    public ExporterResolver(List<Exporter> exporters) {
        this.exporters = exporters;
    }

    public Exporter resolve(ExportFormat format) {
        return exporters.stream()
            .filter(exporter -> exporter.getFormat() == format)
            .findFirst()
            .orElseThrow(() -> new UnsupportedExportFormatException("Unsupported export format: " + format));
    }
}
