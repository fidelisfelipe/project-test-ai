package com.flightmonitor.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a price history snapshot for a given route and date.
 */
@Entity
@Table(name = "price_history", indexes = {
        @Index(name = "idx_price_history_route_date", columnList = "origin, destination, departureDate, recordedAt")
})
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(nullable = false)
    private LocalDate departureDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal minPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal avgPrice;

    private int offersCount;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(length = 10)
    private String trend;

    public PriceHistory() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public int getOffersCount() {
        return offersCount;
    }

    public void setOffersCount(int offersCount) {
        this.offersCount = offersCount;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public String getTrend() {
        return trend;
    }

    public void setTrend(String trend) {
        this.trend = trend;
    }
}
