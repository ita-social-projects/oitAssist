package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.dao.dto.ParticipantResult;
import com.itasocialacademy.oitassist.export.dao.dto.StageResult;
import com.itasocialacademy.oitassist.export.dao.dto.TourResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ExcelExporterTest {
    private final ExcelExporter exporter = new ExcelExporter();

    @Test
    void export_ShouldCreateAllSheets_WhenDataHasToursAndStages() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 30, List.of(
                new StageResult("Етап 1", 30, List.of(
                    new TourResult("Тур 1", 30)))))));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            assertNotNull(workbook.getSheet("Тури"));
            assertNotNull(workbook.getSheet("Етапи"));
            assertNotNull(workbook.getSheet("Спільне"));
        }
    }

    @Test
    void export_ShouldNotCreateToursSheet_WhenNoTours() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 30, List.of(
                new StageResult("Етап 1", 30, List.of())))));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            assertNull(workbook.getSheet("Тури"));
            assertNotNull(workbook.getSheet("Етапи"));
            assertNotNull(workbook.getSheet("Спільне"));
        }
    }

    @Test
    void export_ShouldCreateOnlyTotalSheet_WhenNoStages() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 0, List.of())));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            assertNull(workbook.getSheet("Тури"));
            assertNull(workbook.getSheet("Етапи"));
            assertNotNull(workbook.getSheet("Спільне"));
        }
    }

    @Test
    void export_ShouldFillToursSheetWithCorrectData_WhenDataProvided() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 30, List.of(
                new StageResult("Етап 1", 30, List.of(
                    new TourResult("Тур 1", 12)))))));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Тури");

            Row header = sheet.getRow(0);
            assertEquals("Учасник", header.getCell(0).getStringCellValue());
            assertEquals("Етап", header.getCell(1).getStringCellValue());
            assertEquals("Тур", header.getCell(2).getStringCellValue());
            assertEquals("Бал", header.getCell(3).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals("Ігор", dataRow.getCell(0).getStringCellValue());
            assertEquals("Етап 1", dataRow.getCell(1).getStringCellValue());
            assertEquals("Тур 1", dataRow.getCell(2).getStringCellValue());
            assertEquals(12, dataRow.getCell(3).getNumericCellValue());
        }
    }

    @Test
    void export_ShouldFillStagesSheetWithCorrectData_WhenDataProvided() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 30, List.of(
                new StageResult("Етап 1", 25, List.of(
                    new TourResult("Тур 1", 25)))))));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Етапи");

            Row header = sheet.getRow(0);
            assertEquals("Учасник", header.getCell(0).getStringCellValue());
            assertEquals("Етап", header.getCell(1).getStringCellValue());
            assertEquals("Бал за етап", header.getCell(2).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals("Ігор", dataRow.getCell(0).getStringCellValue());
            assertEquals("Етап 1", dataRow.getCell(1).getStringCellValue());
            assertEquals(25, dataRow.getCell(2).getNumericCellValue());
        }
    }

    @Test
    void export_ShouldFillTotalSheetWithCorrectData_WhenDataProvided() throws IOException {
        ExportData data = new ExportData("Олімпіада", List.of(
            new ParticipantResult("Ігор", 55, List.of())));

        byte[] result = exporter.export(data);

        try (Workbook workbook = openWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Спільне");

            Row header = sheet.getRow(0);
            assertEquals("Учасник", header.getCell(0).getStringCellValue());
            assertEquals("Загальний бал", header.getCell(1).getStringCellValue());

            Row dataRow = sheet.getRow(1);
            assertEquals("Ігор", dataRow.getCell(0).getStringCellValue());
            assertEquals(55, dataRow.getCell(1).getNumericCellValue());
        }
    }

    private Workbook openWorkbook(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }
}
