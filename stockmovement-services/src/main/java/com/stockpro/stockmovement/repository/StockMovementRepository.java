package com.stockpro.stockmovement.repository;

import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByWarehouseIdOrderByTimestampDesc(Long warehouseId);

    List<StockMovement> findByProductIdOrderByTimestampDesc(Long productId);

    List<StockMovement> findByWarehouseIdAndProductIdOrderByTimestampDesc(Long warehouseId, Long productId);

    // Global filtered query - all params optional
    @Query("""
        SELECT m FROM StockMovement m
        WHERE (:warehouseId IS NULL OR m.warehouseId = :warehouseId)
          AND (:productId   IS NULL OR m.productId   = :productId)
          AND (:type        IS NULL OR m.type        = :type)
          AND (:from        IS NULL OR m.timestamp  >= :from)
          AND (:to          IS NULL OR m.timestamp  <= :to)
        ORDER BY m.timestamp DESC
    """)
    List<StockMovement> findFiltered(
        @Param("warehouseId") Long warehouseId,
        @Param("productId")   Long productId,
        @Param("type")        MovementType type,
        @Param("from")        LocalDateTime from,
        @Param("to")          LocalDateTime to
    );
}
