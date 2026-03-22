package com.flightmonitor.controller;

import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.request.FlightSearchRequest;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.dto.response.PriceHistoryResponse;
import com.flightmonitor.service.FlightSearchService;
import com.flightmonitor.service.PriceHistoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * Thymeleaf web controller for browser-based flight search views.
 */
@Controller
public class FlightWebController {

    private final FlightSearchService flightSearchService;
    private final PriceHistoryService priceHistoryService;

    public FlightWebController(FlightSearchService flightSearchService, PriceHistoryService priceHistoryService) {
        this.flightSearchService = flightSearchService;
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * Redirects root to the flight search page.
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/flights/search";
    }

    /**
     * Shows the flight search form.
     *
     * @param model the Spring MVC model
     * @return the search template name
     */
    @GetMapping("/flights/search")
    public String showSearch(Model model) {
        model.addAttribute("cabinClasses", CabinClass.values());
        return "flights/search";
    }

    /**
     * Processes the flight search form submission and redirects to results.
     *
     * @param origin        origin IATA code
     * @param destination   destination IATA code
     * @param departureDate departure date
     * @param returnDate    optional return date
     * @param adults        number of adults
     * @param cabinClass    cabin class
     * @param model         the Spring MVC model
     * @return redirect to results page
     */
    @PostMapping("/flights/search")
    public String processSearch(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(required = false) CabinClass cabinClass,
            Model model) {

        FlightSearchRequest request = new FlightSearchRequest(origin, destination, departureDate, returnDate, adults, cabinClass);
        List<FlightOfferResponse> results = flightSearchService.searchFlights(request);
        model.addAttribute("results", results);
        model.addAttribute("origin", origin);
        model.addAttribute("destination", destination);
        model.addAttribute("departureDate", departureDate);
        return "flights/results";
    }

    /**
     * Shows the flight search results page.
     *
     * @param model the Spring MVC model
     * @return the results template name
     */
    @GetMapping("/flights/results")
    public String showResults(Model model) {
        return "flights/results";
    }

    /**
     * Shows the price history page for a route.
     *
     * @param origin      origin IATA code
     * @param destination destination IATA code
     * @param days        number of days to look back
     * @param model       the Spring MVC model
     * @return the history template name
     */
    @GetMapping("/flights/history")
    public String showHistory(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(defaultValue = "30") int days,
            Model model) {

        if (origin != null && destination != null) {
            List<PriceHistoryResponse> history = priceHistoryService.getHistory(origin, destination, days);
            model.addAttribute("history", history);
            model.addAttribute("origin", origin);
            model.addAttribute("destination", destination);
        }
        return "flights/history";
    }
}
