package com.stockpro.stockmovement.entity;

public enum MovementType {
    IN,          // Stock In — Goods Received Note (GRN) against a PO
    OUT,         // Stock Out — General removal
    TRANSFER,    // Transfer between warehouses
    ADJUSTMENT,  // Manual correction (corrections, damaged goods)
    ISSUE,       // Consumption: SALES / PRODUCTION / INTERNAL_USE
    WRITE_OFF,   // Damaged or expired goods (PDF §2.6)
    RETURN       // Return from supplier or customer (PDF §2.6)
}
