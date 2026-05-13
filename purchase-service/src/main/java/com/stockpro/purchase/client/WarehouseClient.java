package com.stockpro.purchase.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {

    @PostMapping("/api/v1/warehouses/{warehouseId}/stock/add")
    void addStock(
            @PathVariable("warehouseId") Long warehouseId,
            @RequestParam("productId") Long productId,
            @RequestParam("delta") int delta,
            @RequestParam("reason") String reason
    );

    // ✅ Used to resolve department name → warehouseId for PO scoping
    @GetMapping("/api/v1/warehouses")
    List<Map<String, Object>> getAllWarehouses();
}