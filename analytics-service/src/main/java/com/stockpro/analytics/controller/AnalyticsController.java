package com.stockpro.analytics.controller;

import com.stockpro.analytics.dto.*;
import com.stockpro.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Reporting", description = "PDF §2.8 — Inventory analytics, KPIs, and CSV exports")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ─────────────────────────────────────────────────────
    // 1. Total Inventory Valuation
    // ─────────────────────────────────────────────────────
    @GetMapping("/valuation")
    @Operation(summary = "Total inventory value (qty × cost price) across all warehouses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<Double> getGlobalValuation() {
        return ResponseEntity.ok(analyticsService.calculateGlobalValuation());
    }

    // ─────────────────────────────────────────────────────
    // 2. Top Moving Products
    // ─────────────────────────────────────────────────────
    @GetMapping("/top-moving")
    @Operation(summary = "Top N products by inventory turnover rate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<List<ProductPerformanceDTO>> getTopMoving(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getTopMovingProducts(limit));
    }

    // ─────────────────────────────────────────────────────
    // 3. Slow Moving Products
    // ─────────────────────────────────────────────────────
    @GetMapping("/slow-moving")
    @Operation(summary = "Slow-moving products (SLOW_MOVING + VERY_SLOW), ordered by lowest turnover rate first")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<List<ProductPerformanceDTO>> getSlowMoving() {
        return ResponseEntity.ok(analyticsService.getSlowMovingProducts());
    }

    // ─────────────────────────────────────────────────────
    // 4. Dead Stock Report
    // ─────────────────────────────────────────────────────
    @GetMapping("/dead-stock")
    @Operation(summary = "Products with zero movement (dead stock)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<List<ProductPerformanceDTO>> getDeadStock() {
        return ResponseEntity.ok(analyticsService.getDeadStock());
    }

    // ─────────────────────────────────────────────────────
    // 4. Dashboard Metrics (single call for React)
    // ─────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    @Operation(summary = "Full dashboard: valuation, warehouse stats, product performance")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF', 'OFFICER')")
    public ResponseEntity<Object> getDashboard() {
        try {
            return ResponseEntity.ok(analyticsService.getGlobalDashboardMetrics());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Analytics Error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // 5. Warehouse Utilization 
    // ─────────────────────────────────────────────────────
    @GetMapping("/warehouses/utilization")
    @Operation(summary = "Warehouse capacity utilization with NORMAL/HIGH/CRITICAL status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<List<WarehouseUtilizationDTO>> getWarehouseUtilization() {
        return ResponseEntity.ok(analyticsService.getWarehouseUtilization());
    }

    // ─────────────────────────────────────────────────────
    // 6. Supplier Spend Analytics 
    // ─────────────────────────────────────────────────────
    @GetMapping("/supplier-spend")
    @Operation(summary = "Supplier spend rankings from received POs (desc by total spend)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<SupplierSpendDTO>> getSupplierSpend() {
        return ResponseEntity.ok(analyticsService.getSupplierSpend());
    }

    // ─────────────────────────────────────────────────────
    // 7. CSV Exports
    // ─────────────────────────────────────────────────────
    @GetMapping("/export/valuation")
    @Operation(summary = "Download inventory valuation as CSV")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void exportValuationCsv(HttpServletResponse response) throws IOException {
        analyticsService.exportValuationCsv(response);
    }

    @GetMapping("/export/dead-stock")
    @Operation(summary = "Download dead-stock report as CSV")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void exportDeadStockCsv(HttpServletResponse response) throws IOException {
        analyticsService.exportDeadStockCsv(response);
    }

    @GetMapping("/export/top-moving")
    @Operation(summary = "Download top-moving products report as CSV")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void exportTopMovingCsv(HttpServletResponse response) throws IOException {
        analyticsService.exportTopMovingCsv(response);
    }

    @GetMapping("/export/slow-moving")
    @Operation(summary = "Download slow-moving products report as CSV (SLOW_MOVING + VERY_SLOW)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void exportSlowMovingCsv(HttpServletResponse response) throws IOException {
        analyticsService.exportSlowMovingCsv(response);
    }
}
