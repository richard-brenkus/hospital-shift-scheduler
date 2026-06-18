package com.richardbrenkus.shiftschedulermodernized.controller;

import com.richardbrenkus.shiftschedulermodernized.dto.view.MonthlyScheduleExportData;
import com.richardbrenkus.shiftschedulermodernized.service.MonthlyScheduleService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

@Controller
public class SpreadSheetController {

    private final MonthlyScheduleService monthlyScheduleService;

    public SpreadSheetController(MonthlyScheduleService monthlyScheduleService) {
        this.monthlyScheduleService = monthlyScheduleService;
    }

    @PostMapping("/admin/download/excel")
    public void exportToExcel(HttpServletResponse response, @RequestParam("year") int year, @RequestParam("month") int month) throws IOException {
        MonthlyScheduleExportData exportData = monthlyScheduleService.getCalendarExportData(year, month);
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=schedule_" + exportData.monthString() + "_" + exportData.yearString() + ".xlsx";
        response.setHeader(headerKey, headerValue);

        /*ExcelSchedule excelSchedule = new ExcelSchedule(exportData.calendarObjects());
        excelSchedule.export(response);*/
    }
}
