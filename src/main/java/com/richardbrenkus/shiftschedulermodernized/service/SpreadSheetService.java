package com.richardbrenkus.shiftschedulermodernized.service;

import com.richardbrenkus.shiftschedulermodernized.entity.MonthlySchedule;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/*import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SpreadSheetService {

    private final MonthlySchedule monthlySchedule;

    private final XSSFWorkbook workbook;
    private XSSFSheet sheet;

    public ExcelSchedule(MonthlySchedule monthlySchedule) {
        this.monthlySchedule = monthlySchedule;
        workbook = new XSSFWorkbook();
    }

    private void writeHeaderLine() {

        sheet = workbook.createSheet("Schedule");

        Row row = sheet.createRow(0);

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);

        ExcelCellUtils.createCell(sheet, row, 0, "Day", style);
        ExcelCellUtils.createCell(sheet, row, 1, "Shift type 1", style);
        ExcelCellUtils.createCell(sheet, row, 2, "Shift type 2", style);
        ExcelCellUtils.createCell(sheet, row, 3, "Shift type 3", style);
        ExcelCellUtils.createCell(sheet, row, 4, "Shift type 4", style);
        ExcelCellUtils.createCell(sheet, row, 5, "Shift type 5", style);
        ExcelCellUtils.createCell(sheet, row, 6, "Shift type 6", style);
    }

    private void writeDataLines() {
        int rowCount = 1;

        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeight(14);
        style.setFont(font);

        for (CalendarObjectDB calendarObjectDB : CalendarObjectDBList) {
            Row row = sheet.createRow(rowCount++);
            int columnCount = 0;

            ExcelCellUtils.createCell(sheet, row, columnCount++, calendarObjectDB.getDayInteger().toString(), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 1), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 2), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 3), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 4), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 5), style);
            ExcelCellUtils.createCell(sheet, row, columnCount++, addTitleToName(calendarObjectDB, 6), style);
        }
    }

    public void export(HttpServletResponse response) throws IOException {
        writeHeaderLine();
        writeDataLines();

        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();

        outputStream.close();
    }

    public XSSFWorkbook getWorkbook() {
        writeHeaderLine();
        writeDataLines();
        return workbook;
    }

    public String addTitleToName(CalendarObjectDB calendarObjectDB, int shiftType) {
        String titleAndName = "";

        if (shiftType == 1) {
            titleAndName = calendarObjectDB.getShiftType1();
            if (calendarObjectDB.getTitleShiftType1() != null)
                if (!calendarObjectDB.getTitleShiftType1().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType1() + " " + titleAndName;
        } else if (shiftType == 2) {
            titleAndName = calendarObjectDB.getShiftType2();
            if (calendarObjectDB.getTitleShiftType2() != null)
                if (!calendarObjectDB.getTitleShiftType2().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType2() + " " + titleAndName;
        } else if (shiftType == 3) {
            titleAndName = calendarObjectDB.getShiftType3();
            if (calendarObjectDB.getTitleShiftType3() != null)
                if (!calendarObjectDB.getTitleShiftType3().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType3() + " " + titleAndName;
        }
        if (shiftType == 4) {
            titleAndName = calendarObjectDB.getShiftType4();
            if (calendarObjectDB.getTitleShiftType4() != null)
                if (!calendarObjectDB.getTitleShiftType4().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType4() + " " + titleAndName;
        } else if (shiftType == 5) {
            titleAndName = calendarObjectDB.getShiftType5();
            if (calendarObjectDB.getTitleShiftType5() != null)
                if (!calendarObjectDB.getTitleShiftType5().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType5() + " " + titleAndName;
        } else if (shiftType == 6) {
            titleAndName = calendarObjectDB.getShiftType6();
            if (calendarObjectDB.getTitleShiftType6() != null)
                if (!calendarObjectDB.getTitleShiftType6().isEmpty())
                    titleAndName = calendarObjectDB.getTitleShiftType6() + " " + titleAndName;
        }

        return titleAndName;
    }

}
*/