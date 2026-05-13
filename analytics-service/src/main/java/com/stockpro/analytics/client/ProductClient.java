package com.stockpro.analytics.client;

import com.stockpro.analytics.config.FeignConfig;
import com.stockpro.analytics.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", configuration = FeignConfig.class)
public interface ProductClient {
    
    @GetMapping("/api/v1/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

    // Calls /all (flat list) instead of / (paginated Page<>) to avoid deserialization failure
    @GetMapping("/api/v1/products/all")
    List<ProductDTO> getAllProducts();
}
