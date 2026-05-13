package com.stockpro.stockmovement.service.impl;

import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;
import com.stockpro.stockmovement.repository.StockMovementRepository;
import com.stockpro.stockmovement.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository movementRepo;

    // Called by warehouse-service via Feign to save each stock change
    @Override
    public StockMovement saveMovement(StockMovement movement) {
        return movementRepo.save(movement);
    }

    // All movements for a specific warehouse, latest first
    @Override
    public List<StockMovement> getByWarehouse(Long warehouseId) {
        return movementRepo.findByWarehouseIdOrderByTimestampDesc(warehouseId);
    }

    // All movements for a specific product across all warehouses
    @Override
    public List<StockMovement> getByProduct(Long productId) {
        return movementRepo.findByProductIdOrderByTimestampDesc(productId);
    }

    // Movements for a product in a specific warehouse
    @Override
    public List<StockMovement> getByWarehouseAndProduct(Long warehouseId, Long productId) {
        return movementRepo.findByWarehouseIdAndProductIdOrderByTimestampDesc(warehouseId, productId);
    }

    // Global filtered query - used by MovementsPage frontend
    @Override
    public List<StockMovement> getFiltered(Long warehouseId, Long productId, MovementType type,
                                           LocalDateTime from, LocalDateTime to) {
        return movementRepo.findFiltered(warehouseId, productId, type, from, to);
    }
}
