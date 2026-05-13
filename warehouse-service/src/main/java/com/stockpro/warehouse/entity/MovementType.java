package com.stockpro.warehouse.entity;

public enum MovementType {
    IN,                    // Stock In — Goods Received Note (GRN) against a PO
    OUT,                   // Stock Out — General removal
    TRANSFER,              // Transfer between warehouses
    ADJUSTMENT,            // Manual correction (write-offs, corrections)
    ISSUE,                 // Consumption: SALES / PRODUCTION / INTERNAL_USE
    WRITE_OFF,             // §2.6: Damaged or expired goods removed from inventory
    RETURN                 // §2.6: Return from supplier or customer
}
