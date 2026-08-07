package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.StageResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.TourResult;
import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.enums.ExportFormat;
import com.itasocialacademy.oitassist.export.exceptions.ExcelExportException;
import com.itasocialacademy.oitassist.export.service.interfaces.Exporter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelExporter implements Exporter {
    private static final String NO_SCORE = "-";

    @Override
    public byte[] export(ExportData data) {
        try (Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (hasTours(data)) {
                createToursSheet(workbook, data);
            }
            if (hasStages(data)) {
                createStagesSheet(workbook, data);
            }
            createTotalSheet(workbook, data);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ExcelExportException("Failed to export Excel file", e);
        }
    }

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.EXCEL;
    }

    private void createToursSheet(Workbook workbook, ExportData data) {
        Sheet sheet = createSheetWithHeader(workbook, "Тури", "Учасник", "Етап", "Тур", "Бал");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            for (StageResult stage : participant.stages()) {
                for (TourResult tour : stage.tours()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(participant.participantName());
                    row.createCell(1).setCellValue(stage.stageTitle());
                    row.createCell(2).setCellValue(tour.tourTitle());
                    if (tour.tourScore() == null) {
                        row.createCell(3).setCellValue(NO_SCORE);
                    } else {
                        row.createCell(3).setCellValue(tour.tourScore());
                    }
                }
            }
        }

        applyAutoFilter(sheet, rowIndex);
    }

    private void createStagesSheet(Workbook workbook, ExportData data) {
        Sheet sheet = createSheetWithHeader(workbook, "Етапи", "Учасник", "Етап", "Бал за етап");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            for (StageResult stage : participant.stages()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(participant.participantName());
                row.createCell(1).setCellValue(stage.stageTitle());
                if (stage.stageScore() == null) {
                    row.createCell(2).setCellValue(NO_SCORE);
                } else {
                    row.createCell(2).setCellValue(stage.stageScore());
                }
            }
        }

        applyAutoFilter(sheet, rowIndex);
    }

    private void createTotalSheet(Workbook workbook, ExportData data) {
        Sheet sheet = createSheetWithHeader(workbook, "Спільне", "Учасник", "Загальний бал");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(participant.participantName());
            if (participant.totalScore() == null) {
                row.createCell(1).setCellValue(NO_SCORE);
            } else {
                row.createCell(1).setCellValue(participant.totalScore());
            }
        }

        applyAutoFilter(sheet, rowIndex);
    }

    private Sheet createSheetWithHeader(Workbook workbook, String sheetName, String... columnTitles) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnTitles.length; i++) {
            header.createCell(i).setCellValue(columnTitles[i]);
        }
        return sheet;
    }

    private void applyAutoFilter(Sheet sheet, int lastRowIndex) {
        int lastColumn = sheet.getRow(0).getLastCellNum() - 1;
        sheet.setAutoFilter(new CellRangeAddress(0, lastRowIndex, 0, lastColumn));
    }

    private boolean hasStages(ExportData data) {
        return data.participants().stream()
            .anyMatch(participant -> !participant.stages().isEmpty());
    }

    private boolean hasTours(ExportData data) {
        return data.participants().stream()
            .flatMap(participant -> participant.stages().stream())
            .anyMatch(stage -> !stage.tours().isEmpty());
    }
}
