package com.stockpro.warehouse.client;

import com.stockpro.warehouse.entity.StockMovement;
import com.stockpro.warehouse.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Calls stock-movement-service to record each movement
// NOTE: /record endpoint is permitAll in movement service SecurityConfig
@FeignClient(name = "stockmovement-services", configuration = FeignConfig.class)
public interface MovementClient {

    @PostMapping("/api/v1/movements/record")
    StockMovement saveMovement(@RequestBody StockMovement movement);
}
