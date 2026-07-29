package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import com.richardbrenkus.shiftschedulermodernized.util.ExcelCellUtils;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SpreadsheetService {

    private static final DateTimeFormatter MONTH_YEAR_ID_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("MM_yyyy");

    private final StoredScheduleDayRepository storedScheduleDayRepository;
    private final ShiftTypeService shiftTypeService;
    private final MessageSource messageSource;

    @Transactional(readOnly = true)
    public void writeSavedSchedule(OutputStream outputStream, YearMonth selectedMonth) throws IOException {
        if (selectedMonth == null) {
            throw new IllegalArgumentException("Selected month must not be null.");
        }

        Locale locale = LocaleContextHolder.getLocale();

        List<StoredScheduleDay> storedDays = loadStoredScheduleDays(selectedMonth);
        List<Integer> shiftTypes = shiftTypeService.getShiftTypes();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet(message("spreadsheet.schedule.sheetName", locale));

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            writeHeaderLine(sheet, headerStyle, shiftTypes, locale);
            writeDataLines(sheet, dataStyle, storedDays, shiftTypes);
            autoSizeColumns(sheet, shiftTypes.size() + 1);

            workbook.write(outputStream);
        }
    }

    public String createFileName(YearMonth selectedMonth) {
        if (selectedMonth == null) {
            return "schedule.xlsx";
        }

        return "schedule_" + selectedMonth.format(FILE_NAME_FORMATTER) + ".xlsx";
    }

    private List<StoredScheduleDay> loadStoredScheduleDays(YearMonth selectedMonth) {
        String monthYearId = selectedMonth.format(MONTH_YEAR_ID_FORMATTER);

        return storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc(monthYearId).stream().sorted(Comparator.comparing(StoredScheduleDay::getDayInteger)).toList();
    }

    private void writeHeaderLine(XSSFSheet sheet, CellStyle style, List<Integer> shiftTypes, Locale locale) {
        Row row = sheet.createRow(0);

        ExcelCellUtils.createCell(row, 0, message("spreadsheet.schedule.column.day", locale), style);

        int columnIndex = 1;
        for (Integer shiftType : shiftTypes) {
            ExcelCellUtils.createCell(row, columnIndex++, message("spreadsheet.schedule.column.shiftType", new Object[]{shiftType}, locale), style);
        }
    }

    private void writeDataLines(XSSFSheet sheet, CellStyle style, List<StoredScheduleDay> storedDays, List<Integer> shiftTypes) {
        int rowIndex = 1;

        for (StoredScheduleDay storedDay : storedDays) {
            Row row = sheet.createRow(rowIndex++);

            ExcelCellUtils.createCell(row, 0, storedDay.getDayInteger() == null || storedDay.getDayInteger() == 0 ? "" : storedDay.getDayInteger().toString(), style);

            int columnIndex = 1;
            for (Integer shiftType : shiftTypes) {
                ExcelCellUtils.createCell(row, columnIndex++, displayNameForShiftType(storedDay, shiftType), style);
            }
        }
    }

    private String displayNameForShiftType(StoredScheduleDay storedDay, int shiftType) {
        if (storedDay == null || storedDay.getAssignmentsByShiftType() == null || !storedDay.getAssignmentsByShiftType().containsKey(shiftType)) {
            return "";
        }

        StoredUserSnapshot userSnapshot = storedDay.getAssignmentsByShiftType().get(shiftType);

        if (userSnapshot == null) {
            return "";
        }

        String name = userSnapshot.getName() == null ? "" : userSnapshot.getName();
        String title = userSnapshot.getTitle() == null ? "" : userSnapshot.getTitle();

        return title.isBlank() ? name : title + " " + name;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);

        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);

        style.setFont(font);
        return style;
    }

    private void autoSizeColumns(XSSFSheet sheet, int columnCount) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }
    }

    private String message(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

    private String message(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, locale);
    }
}
