package com.stockpro.alert.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    /**
     * GET /api/v1/alerts                              → all alerts
     * GET /api/v1/alerts?type=LOW_STOCK               → filter by type
     * GET /api/v1/alerts?severity=CRITICAL             → filter by severity
     * GET /api/v1/alerts?acknowledged=false            → unacknowledged only
     * GET /api/v1/alerts?type=LOW_STOCK&severity=CRITICAL&acknowledged=false → combined
     */
    @GetMapping
    public List<Alert> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean acknowledged) {
        return service.getFilteredAlerts(type, severity, acknowledged);
    }

    @PutMapping("/{id}/acknowledge")
    public Alert ack(@PathVariable Long id) {
        return service.acknowledgeAlert(id);
    }

    @PutMapping("/{id}/read")
    public Alert read(@PathVariable Long id) {
        return service.markAsRead(id);
    }

    @GetMapping("/count/unread")
    public long count() {
        return service.getUnreadCount();
    }

    /** Returns pending (unacknowledged) STOCK_TRANSFER alerts for a specific destination warehouse. */
    @GetMapping("/warehouse/{warehouseId}/transfers")
    public List<Alert> getPendingTransfers(@PathVariable Long warehouseId) {
        return service.getPendingTransferAlerts(warehouseId);
    }
}