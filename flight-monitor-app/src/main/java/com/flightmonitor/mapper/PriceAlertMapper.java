package com.flightmonitor.mapper;

import com.flightmonitor.domain.entity.PriceAlert;
import com.flightmonitor.dto.response.PriceAlertResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting PriceAlert entities to response DTOs.
 */
@Component
public class PriceAlertMapper {

    /**
     * Converts a PriceAlert entity to a PriceAlertResponse DTO.
     *
     * @param alert the entity to convert
     * @return the response DTO
     */
    public PriceAlertResponse toResponse(PriceAlert alert) {
        return new PriceAlertResponse(
                alert.getId(),
                alert.getUserEmail(),
                alert.getOrigin(),
                alert.getDestination(),
                alert.getDepartureDate(),
                alert.getTargetPrice(),
                alert.getStatus(),
                alert.getTriggeredAt(),
                alert.getCreatedAt(),
                alert.getLastCheckedAt()
        );
    }
}
