package com.stockpro.analytics.repository;

import com.stockpro.analytics.entity.ProductPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductPerformanceRepository extends JpaRepository<ProductPerformance, Long> {

    List<ProductPerformance> findByMovementCategory(String category);

    List<ProductPerformance> findTop10ByOrderByTurnoverRateDesc();

    List<ProductPerformance> findByMovementCategoryIn(List<String> categories);

    List<ProductPerformance> findByMovementCategoryInOrderByTurnoverRateAsc(List<String> categories);

    /**
     * Dead stock = no movement in 90+ days.
     * Includes products where lastMovementDate IS NULL (never moved) OR older than cutoff.
     */
    @Query("SELECT p FROM ProductPerformance p WHERE p.lastMovementDate IS NULL OR p.lastMovementDate < :cutoff")
    List<ProductPerformance> findDeadStockBefore(@Param("cutoff") LocalDateTime cutoff);
}
