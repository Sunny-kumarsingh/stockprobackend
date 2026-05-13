package com.stockpro.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "warehouse-service", path = "/api/v1/warehouses")
public interface WarehouseClient {

    @PostMapping("/{warehouseId}/stock")
    void updateStock(
        @PathVariable("warehouseId") Long warehouseId,
        @RequestParam("productId") Long productId,
        @RequestParam("quantity") int quantity,
        @RequestParam("reason") String reason
    );
}
