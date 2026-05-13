package com.stockpro.stockmovement.service;

import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementService {

    // Save a new movement (called by warehouse-service via Feign)
    StockMovement saveMovement(StockMovement movement);

    // Query methods for frontend
    List<StockMovement> getByWarehouse(Long warehouseId);
    List<StockMovement> getByProduct(Long productId);
    List<StockMovement> getByWarehouseAndProduct(Long warehouseId, Long productId);
    List<StockMovement> getFiltered(Long warehouseId, Long productId, MovementType type,
                                    LocalDateTime from, LocalDateTime to);
}
