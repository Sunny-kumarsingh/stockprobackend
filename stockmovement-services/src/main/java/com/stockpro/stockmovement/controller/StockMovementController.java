package com.stockpro.stockmovement.controller;

import com.stockpro.stockmovement.entity.MovementType;
import com.stockpro.stockmovement.entity.StockMovement;
import com.stockpro.stockmovement.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService movementService;

    // INTERNAL: Called by warehouse-service via Feign — no user JWT
    @Operation(summary = "Save a stock movement (internal service call)", security = {})
    @PostMapping("/record")
    public StockMovement saveMovement(@RequestBody StockMovement movement) {
        return movementService.saveMovement(movement);
    }

    // GET all movements with optional filters (warehouseId, productId, type, from, to)
    // Used by frontend MovementsPage
    @GetMapping
    public List<StockMovement> getFiltered(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime fromDt = (from != null && !from.isEmpty()) ? LocalDateTime.parse(from) : null;
        LocalDateTime toDt   = (to   != null && !to.isEmpty())   ? LocalDateTime.parse(to)   : null;
        return movementService.getFiltered(warehouseId, productId, type, fromDt, toDt);
    }

    // GET all movements for a specific warehouse
    @GetMapping("/warehouse/{warehouseId}")
    public List<StockMovement> getByWarehouse(@PathVariable Long warehouseId) {
        return movementService.getByWarehouse(warehouseId);
    }

    // GET all movements for a specific product (across all warehouses)
    @GetMapping("/product/{productId}")
    public List<StockMovement> getByProduct(@PathVariable Long productId) {
        return movementService.getByProduct(productId);
    }

    // GET movements for a product in a specific warehouse
    @GetMapping("/warehouse/{warehouseId}/product/{productId}")
    public List<StockMovement> getByWarehouseAndProduct(@PathVariable Long warehouseId,
                                                        @PathVariable Long productId) {
        return movementService.getByWarehouseAndProduct(warehouseId, productId);
    }
}
