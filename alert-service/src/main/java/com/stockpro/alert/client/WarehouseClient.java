package com.stockpro.alert.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "warehouse-service", configuration = com.stockpro.alert.config.FeignConfig.class)
public interface WarehouseClient {

    @GetMapping("/api/v1/warehouses/{id}")
    Map<String, Object> getWarehouseById(@PathVariable("id") Long id);
}
