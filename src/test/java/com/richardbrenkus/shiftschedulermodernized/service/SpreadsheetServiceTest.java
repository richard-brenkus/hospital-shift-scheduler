package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.StoredScheduleDay;
import com.richardbrenkus.shiftschedulermodernized.entity.StoredUserSnapshot;
import com.richardbrenkus.shiftschedulermodernized.repository.StoredScheduleDayRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpreadsheetServiceTest {

    @Mock
    private StoredScheduleDayRepository storedScheduleDayRepository;

    @Mock
    private ShiftTypeService shiftTypeService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private SpreadsheetService service;

    @Test
    void shouldReturnDefaultFileName_whenMonthIsNull() {
        assertThat(service.createFileName(null)).isEqualTo("schedule.xlsx");
    }

    @Test
    void shouldFormatFileNameFromMonth() {
        assertThat(service.createFileName(YearMonth.of(2026, 8))).isEqualTo("schedule_08_2026.xlsx");
    }

    @Test
    void shouldThrowIllegalArgumentException_whenWritingScheduleForNullMonth() {
        assertThatThrownBy(() -> service.writeSavedSchedule(new ByteArrayOutputStream(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldQueryRepositoryUsingMonthYearIdAndWriteXlsxWithOneRowPerDay() throws Exception {
        YearMonth month = YearMonth.of(2026, 8);
        StoredScheduleDay day1 = StoredScheduleDay.builder().dateId(20260801L).monthYearId("08/2026").dayInteger(1).assignmentsByShiftType(assignments(1, new StoredUserSnapshot(10L, "freddie", "Freddie", "MUDr."))).build();
        StoredScheduleDay day2 = StoredScheduleDay.builder().dateId(20260802L).monthYearId("08/2026").dayInteger(2).assignmentsByShiftType(new HashMap<>()).build();
        when(storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026")).thenReturn(List.of(day1, day2));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1, 2));
        when(messageSource.getMessage(eq("spreadsheet.schedule.column.day"), any(), any(Locale.class))).thenReturn("Day");
        when(messageSource.getMessage(eq("spreadsheet.schedule.column.shiftType"), any(Object[].class), any(Locale.class))).thenReturn("Shift");
        when(messageSource.getMessage(eq("spreadsheet.schedule.sheetName"), any(), any(Locale.class))).thenReturn("Schedule");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        service.writeSavedSchedule(stream, month);

        byte[] bytes = stream.toByteArray();
        assertThat(bytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Schedule");
            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Day");
            Row rowDay1 = sheet.getRow(1);
            assertThat(rowDay1.getCell(0).getStringCellValue()).isEqualTo("1");
            assertThat(rowDay1.getCell(1).getStringCellValue()).isEqualTo("MUDr. Freddie");
            Row rowDay2 = sheet.getRow(2);
            assertThat(rowDay2.getCell(0).getStringCellValue()).isEqualTo("2");
            assertThat(rowDay2.getCell(1).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void shouldReturnJustNameWithoutTitle_whenTitleIsBlank() throws Exception {
        YearMonth month = YearMonth.of(2026, 8);
        StoredScheduleDay day = StoredScheduleDay.builder().dateId(20260801L).monthYearId("08/2026").dayInteger(1).assignmentsByShiftType(assignments(1, new StoredUserSnapshot(10L, "kurt", "Kurt", ""))).build();
        when(storedScheduleDayRepository.findByMonthYearIdOrderByDayIntegerAsc("08/2026")).thenReturn(List.of(day));
        when(shiftTypeService.getShiftTypes()).thenReturn(List.of(1));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("X");
        when(messageSource.getMessage(eq("spreadsheet.schedule.column.shiftType"), any(Object[].class), any(Locale.class))).thenReturn("Shift");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        service.writeSavedSchedule(stream, month);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(stream.toByteArray()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue()).isEqualTo("Kurt");
        }
    }

    private Map<Integer, StoredUserSnapshot> assignments(int shiftType, StoredUserSnapshot snapshot) {
        Map<Integer, StoredUserSnapshot> map = new HashMap<>();
        map.put(shiftType, snapshot);
        return map;
    }
}
