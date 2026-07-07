package com.richardbrenkus.shiftschedulermodernized.util;

import com.richardbrenkus.shiftschedulermodernized.dto.export.UserExportRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Component
public class UserExcelExporter {

    private static final String SHEET_NAME = "Users";

    public void export(List<UserExportRecord> users, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeHeader(sheet, headerStyle);
            writeRows(sheet, users, dataStyle);
            autoSizeColumns(sheet, 6);

            workbook.write(outputStream);
        }
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle) {

        Row row = sheet.createRow(0);

        createCell(row, 0, "User ID", headerStyle);
        createCell(row, 1, "E-mail", headerStyle);
        createCell(row, 2, "Name", headerStyle);
        createCell(row, 3, "Username", headerStyle);
        createCell(row, 4, "Role", headerStyle);
        createCell(row, 5, "Enabled", headerStyle);
    }

    private void writeRows(
            Sheet sheet,
            List<UserExportRecord> users,
            CellStyle dataStyle) {

        int rowIndex = 1;

        for (UserExportRecord user : users) {

            Row row = sheet.createRow(rowIndex++);

            createCell(row, 0, user.username(), dataStyle);
            createCell(row, 1, user.email(), dataStyle);
            createCell(row, 2, user.name(), dataStyle);
            createCell(row, 3, user.username(), dataStyle);
            createCell(row, 4, user.role(), dataStyle);
            createCell(row, 5, user.enabled(), dataStyle);
        }
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);

        switch (value) {
            case null -> cell.setCellValue("");
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            default -> cell.setCellValue(value.toString());
        }

        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }
}
