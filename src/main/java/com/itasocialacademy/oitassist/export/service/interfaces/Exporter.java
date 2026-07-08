package com.itasocialacademy.oitassist.export.service.interfaces;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;

public interface Exporter {
    byte[] export(ExportData data);

    ExportFormat getFormat();
}
