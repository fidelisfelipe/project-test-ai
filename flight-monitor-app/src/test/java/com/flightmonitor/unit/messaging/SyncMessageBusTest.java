package com.flightmonitor.unit.messaging;

import com.flightmonitor.domain.enums.CabinClass;
import com.flightmonitor.messaging.BrokerType;
import com.flightmonitor.messaging.MessageHandler;
import com.flightmonitor.messaging.MessageSendResult;
import com.flightmonitor.messaging.dto.FlightSearchRequestMessage;
import com.flightmonitor.messaging.dto.FlightSearchResultMessage;
import com.flightmonitor.messaging.dto.PriceAlertMessage;
import com.flightmonitor.messaging.sync.SyncMessageBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncMessageBusTest {

    @Mock
    private MessageHandler messageHandler;

    private SyncMessageBus bus;

    @BeforeEach
    void setUp() {
        bus = new SyncMessageBus(messageHandler);
    }

    @Test
    void sendSearchRequest_completesImmediately_whenSyncModeActive() throws Exception {
        given(messageHandler.handleSearchRequest(any())).willReturn(mockResult());

        var future = bus.sendSearchRequest(testMessage());

        assertThat(future).isDone();
        assertThat(future.get().success()).isTrue();
        assertThat(future.get().brokerType()).isEqualTo(BrokerType.SYNC.name());
    }

    @Test
    void sendSearchRequest_returnsFailed_whenHandlerThrowsException() throws Exception {
        given(messageHandler.handleSearchRequest(any())).willThrow(new RuntimeException("handler error"));

        var future = bus.sendSearchRequest(testMessage());

        assertThat(future).isDone();
        assertThat(future.get().success()).isFalse();
        assertThat(future.get().errorMessage()).isNotBlank();
    }

    @Test
    void isAvailable_alwaysReturnsTrue() {
        assertThat(bus.isAvailable()).isTrue();
    }

    @Test
    void getBrokerType_returnsSYNC() {
        assertThat(bus.getBrokerType().name()).isEqualTo("SYNC");
    }

    @Test
    void sendPriceAlert_completesImmediately() throws Exception {
        PriceAlertMessage alertMessage = new PriceAlertMessage(
                UUID.randomUUID(), "test@example.com", "BSB", "LIS",
                LocalDate.now().plusDays(5), new BigDecimal("300.00"), new BigDecimal("250.00"),
                LocalDateTime.now());

        var future = bus.sendPriceAlert(alertMessage);

        assertThat(future).isDone();
        assertThat(future.get().success()).isTrue();
        verify(messageHandler).handlePriceAlert(alertMessage);
    }

    @Test
    void sendSearchRequest_callsHandlerWithMessage() throws Exception {
        FlightSearchRequestMessage message = testMessage();
        given(messageHandler.handleSearchRequest(message)).willReturn(mockResult());

        bus.sendSearchRequest(message);

        verify(messageHandler).handleSearchRequest(message);
    }

    private FlightSearchRequestMessage testMessage() {
        return new FlightSearchRequestMessage(
                UUID.randomUUID().toString(), "BSB", "LIS",
                LocalDate.now().plusDays(5), null, 1, CabinClass.ECONOMY);
    }

    private FlightSearchResultMessage mockResult() {
        return new FlightSearchResultMessage("corr-id", List.of());
    }
}
