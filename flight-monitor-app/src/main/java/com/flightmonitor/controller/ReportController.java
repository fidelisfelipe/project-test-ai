package com.flightmonitor.controller;

import com.flightmonitor.dto.response.FlightReportResponse;
import com.flightmonitor.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller for flight price report dashboard.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Shows the report dashboard with an optional pre-loaded report.
     *
     * @param origin      origin IATA code (optional)
     * @param destination destination IATA code (optional)
     * @param startDate   report start date (optional)
     * @param endDate     report end date (optional)
     * @param model       the Spring MVC model
     * @return the dashboard template name
     */
    @GetMapping("/dashboard")
    public String showDashboard(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (origin != null && destination != null && startDate != null && endDate != null) {
            FlightReportResponse report = reportService.generateReport(origin, destination, startDate, endDate);
            model.addAttribute("report", report);
        }
        return "reports/dashboard";
    }
}
