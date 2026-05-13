package com.stockpro.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WarehouseDTO {
    @JsonProperty("warehouseId")
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private Integer usedCapacity;
    private boolean active;
}
