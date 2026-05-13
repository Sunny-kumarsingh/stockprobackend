package com.stockpro.analytics.client;

import java.util.List;

import com.stockpro.analytics.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.stockpro.analytics.dto.StockLevelDTO;
import com.stockpro.analytics.dto.WarehouseDTO;

@FeignClient(name = "warehouse-service", configuration = FeignConfig.class)
public interface WarehouseClient {

    @GetMapping("/api/v1/warehouses")
    List<WarehouseDTO> getAllWarehouses();

    @GetMapping("/api/v1/warehouses/{id}/inventory")
    List<StockLevelDTO> getWarehouseInventory(@PathVariable("id") Long id);
}
