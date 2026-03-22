package com.flightmonitor.service;

import com.flightmonitor.dto.response.FlightReportResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Service implementation for generating flight price reports.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    private final PriceHistoryService priceHistoryService;

    public ReportServiceImpl(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * Generates a price report for a given route and date range.
     *
     * @param origin      the origin IATA code
     * @param destination the destination IATA code
     * @param startDate   the start date of the report range
     * @param endDate     the end date of the report range
     * @return a compiled flight report response
     */
    @Override
    public FlightReportResponse generateReport(String origin, String destination, LocalDate startDate, LocalDate endDate) {
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        List<PriceHistoryResponse> history = priceHistoryService.getHistory(origin, destination, days);

        BigDecimal lowest = history.stream().map(PriceHistoryResponse::minPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal highest = history.stream().map(PriceHistoryResponse::maxPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal average = BigDecimal.ZERO;
        if (!history.isEmpty()) {
            BigDecimal sum = history.stream().map(PriceHistoryResponse::avgPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            average = sum.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);
        }

        String trend = priceHistoryService.calculateTrend(history);
        log.info("Generated report for route {}->{} from {} to {} with {} records", origin, destination, startDate, endDate, history.size());

        return new FlightReportResponse(
                origin,
                destination,
                startDate,
                endDate,
                lowest,
                highest,
                average,
                history.size(),
                trend,
                history
        );
    }
}
