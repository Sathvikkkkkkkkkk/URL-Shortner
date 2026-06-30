package com.urlshortener.repository;

import com.urlshortener.model.ClickAnalytic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickAnalyticRepository extends JpaRepository<ClickAnalytic, Long> {

    /**
     * Group clicks by hour of the day for the specified time range.
     * Returns a list of Object arrays where:
     * - index 0 is the hour of day (Integer, 0-23)
     * - index 1 is the count of clicks (Long)
     */
    @Query("SELECT HOUR(c.clickedAt) as clickHour, COUNT(c.id) as clickCount " +
           "FROM ClickAnalytic c " +
           "WHERE c.clickedAt >= :since " +
           "GROUP BY HOUR(c.clickedAt) " +
           "ORDER BY clickHour ASC")
    List<Object[]> findHourlyClicksSince(@Param("since") LocalDateTime since);
}
