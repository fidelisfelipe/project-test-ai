package com.flightmonitor.unit.controller;

import com.flightmonitor.controller.AlertController;
import com.flightmonitor.controller.FlightSearchController;
import com.flightmonitor.domain.enums.AlertStatus;
import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.dto.response.FlightOfferResponse;
import com.flightmonitor.dto.response.PriceAlertResponse;
import com.flightmonitor.exception.FlightNotFoundException;
import com.flightmonitor.exception.GlobalExceptionHandler;
import com.flightmonitor.service.AlertService;
import com.flightmonitor.service.FlightSearchService;
import com.flightmonitor.service.PriceHistoryService;
import com.flightmonitor.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FlightSearchControllerTest {

    @Mock
    private FlightSearchService flightSearchService;
    @Mock
    private PriceHistoryService priceHistoryService;
    @Mock
    private ReportService reportService;
    @Mock
    private AlertService alertService;

    private MockMvc mockMvc;
    private MockMvc alertMockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        FlightSearchController flightController = new FlightSearchController(flightSearchService, priceHistoryService, reportService);
        mockMvc = MockMvcBuilders.standaloneSetup(flightController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        AlertController alertController = new AlertController(alertService);
        alertMockMvc = MockMvcBuilders.standaloneSetup(alertController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void search_returns200WithResults() throws Exception {
        FlightOfferResponse offer = new FlightOfferResponse(UUID.randomUUID(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, "TP", new BigDecimal("299.99"), "EUR",
                0, "PT5H", CabinClass.ECONOMY, null, LocalDateTime.now(), "AMADEUS");
        when(flightSearchService.searchFlights(any())).thenReturn(List.of(offer));

        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", "BSB")
                        .param("destination", "LIS")
                        .param("departureDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].airline").value("TP"));
    }

    @Test
    void search_returns400ForEmptyOrigin() throws Exception {
        when(flightSearchService.searchFlights(any()))
                .thenThrow(new IllegalArgumentException("Origin must be a valid 3-letter IATA code"));

        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", "")
                        .param("destination", "LIS")
                        .param("departureDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAlert_returns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        PriceAlertResponse response = new PriceAlertResponse(id, "test@test.com", "BSB", "LIS",
                LocalDate.now().plusDays(10), new BigDecimal("299.99"), AlertStatus.ACTIVE, null, LocalDateTime.now(), null);
        when(alertService.createAlert(any())).thenReturn(response);

        String body = objectMapper.writeValueAsString(new com.flightmonitor.dto.request.CreateAlertRequest(
                "test@test.com", "BSB", "LIS", LocalDate.now().plusDays(10), new BigDecimal("299.99")));

        alertMockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void deleteAlert_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(alertService).deleteAlert(id);

        alertMockMvc.perform(delete("/api/v1/alerts/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAlert_returns404ForNonExistentId() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new FlightNotFoundException("Alert not found")).when(alertService).deleteAlert(id);

        alertMockMvc.perform(delete("/api/v1/alerts/" + id))
                .andExpect(status().isNotFound());
    }
}
