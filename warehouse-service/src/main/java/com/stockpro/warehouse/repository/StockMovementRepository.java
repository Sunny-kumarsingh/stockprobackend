package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.MovementType;
import com.stockpro.warehouse.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByWarehouseId(Long warehouseId);

    List<StockMovement> findByProductId(Long productId);
    
    List<StockMovement> findByWarehouseIdOrderByTimestampDesc(Long warehouseId);
    
    List<StockMovement> findByProductIdOrderByTimestampDesc(Long productId);
    
 // Product movements in a specific warehouse
    List<StockMovement> findByWarehouseIdAndProductIdOrderByTimestampDesc(Long warehouseId, Long productId);

    // Global filtered movements (all params optional)
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
        @Param("warehouseId") 
        Long warehouseId,
        @Param("productId") 
        Long productId,
        @Param("type")   
        MovementType type,
        @Param("from")   
        LocalDateTime from,
        @Param("to")      
        LocalDateTime to
    );


}