package com.flightmonitor.repository;

import com.flightmonitor.domain.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for SearchLog entities.
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
}
