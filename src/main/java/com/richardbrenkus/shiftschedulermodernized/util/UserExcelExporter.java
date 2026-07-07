package com.richardbrenkus.shiftschedulermodernized.util;

import com.richardbrenkus.shiftschedulermodernized.dto.export.UserExportRecord;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class UserExcelExporter {

    private static final String SHEET_NAME_MESSAGE_KEY = "spreadsheet.users.sheetName";
    private static final int COLUMN_COUNT = 6;

    private final MessageSource messageSource;

    public void export(List<UserExportRecord> users, OutputStream outputStream, Locale locale) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(message(SHEET_NAME_MESSAGE_KEY, locale));

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeHeader(sheet, headerStyle, locale);
            writeRows(sheet, users, dataStyle);
            autoSizeColumns(sheet, COLUMN_COUNT);

            workbook.write(outputStream);
        }
    }

    private void writeHeader(Sheet sheet, CellStyle headerStyle, Locale locale) {
        Row row = sheet.createRow(0);

        createCell(row, 0, message("spreadsheet.users.column.id", locale), headerStyle);
        createCell(row, 1, message("spreadsheet.users.column.email", locale), headerStyle);
        createCell(row, 2, message("spreadsheet.users.column.name", locale), headerStyle);
        createCell(row, 3, message("spreadsheet.users.column.username", locale), headerStyle);
        createCell(row, 4, message("spreadsheet.users.column.role", locale), headerStyle);
        createCell(row, 5, message("spreadsheet.users.column.enabled", locale), headerStyle);
    }

    private void writeRows(Sheet sheet, List<UserExportRecord> users, CellStyle dataStyle) {
        int rowIndex = 1;

        for (UserExportRecord user : users) {
            Row row = sheet.createRow(rowIndex++);

            createCell(row, 0, user.userId(), dataStyle);
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

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}
