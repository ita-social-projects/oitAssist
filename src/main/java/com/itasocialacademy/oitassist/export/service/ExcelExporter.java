package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.dto.ParticipantResult;
import com.itasocialacademy.oitassist.export.dao.dto.StageResult;
import com.itasocialacademy.oitassist.export.dao.dto.TourResult;
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
        Sheet sheet = workbook.createSheet("Тури");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Учасник");
        header.createCell(1).setCellValue("Етап");
        header.createCell(2).setCellValue("Тур");
        header.createCell(3).setCellValue("Бал");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            for (StageResult stage : participant.stages()) {
                for (TourResult tour : stage.tours()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(participant.participantName());
                    row.createCell(1).setCellValue(stage.stageTitle());
                    row.createCell(2).setCellValue(tour.tourTitle());
                    row.createCell(3).setCellValue(tour.tourScore());
                }
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 3));
    }

    private void createStagesSheet(Workbook workbook, ExportData data) {
        Sheet sheet = workbook.createSheet("Етапи");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Учасник");
        header.createCell(1).setCellValue("Етап");
        header.createCell(2).setCellValue("Бал за етап");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            for (StageResult stage : participant.stages()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(participant.participantName());
                row.createCell(1).setCellValue(stage.stageTitle());
                row.createCell(2).setCellValue(stage.stageScore());
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 2));
    }

    private void createTotalSheet(Workbook workbook, ExportData data) {
        Sheet sheet = workbook.createSheet("Спільне");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Учасник");
        header.createCell(1).setCellValue("Загальний бал");

        int rowIndex = 1;
        for (ParticipantResult participant : data.participants()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(participant.participantName());
            row.createCell(1).setCellValue(participant.totalScore());
        }

        sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, 1));
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
