package com.stockpro.warehouse.client;

import com.stockpro.warehouse.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    Object getProductById(@PathVariable("id") Long id);

    // 🔥 Called by Warehouse Service to sync live stock into Product Service
    @PutMapping("/api/v1/products/{id}/stock")
    void updateTotalStock(@PathVariable("id") Long productId, @RequestParam("total") Integer total);
}

