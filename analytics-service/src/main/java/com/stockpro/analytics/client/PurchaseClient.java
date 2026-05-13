package com.stockpro.analytics.client;

import com.stockpro.analytics.config.FeignConfig;
import com.stockpro.analytics.dto.PurchaseOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "purchase-service", configuration = FeignConfig.class)
public interface PurchaseClient {

    @GetMapping("/api/v1/purchase-orders")
    List<PurchaseOrderDTO> getPurchaseOrders();
}
