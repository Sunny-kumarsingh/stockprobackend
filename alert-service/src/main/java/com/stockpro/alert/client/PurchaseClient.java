package com.stockpro.alert.client;

import com.stockpro.alert.dto.PurchaseOrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "purchase-service", configuration = com.stockpro.alert.config.FeignConfig.class)
public interface PurchaseClient {

    @GetMapping("/api/v1/purchase-orders/overdue")
    List<PurchaseOrderDTO> getOverduePOs();
}
