package com.stockpro.payment.client;

import com.stockpro.payment.config.FeignAuthConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "warehouse-service", configuration = FeignAuthConfig.class)
public interface WarehouseClient {

    @GetMapping("/api/v1/warehouses")
    List<Map<String, Object>> getAllWarehouses();
}
