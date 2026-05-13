package com.stockpro.purchase.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "supplier-service")
public interface SupplierClient {

    @GetMapping("/api/v1/suppliers/{id}/active")
    boolean isSupplierActive(@PathVariable("id") Long supplierId);
}