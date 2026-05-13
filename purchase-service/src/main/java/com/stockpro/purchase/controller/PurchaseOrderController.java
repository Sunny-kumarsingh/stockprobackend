package com.stockpro.purchase.controller;

import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Purchase Order Controller", description = "Endpoints for managing the procurement lifecycle")
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    @PostMapping
    @Operation(summary = "Create a new Purchase Order (Draft)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<PurchaseOrder> createPO(Authentication auth, @RequestBody PurchaseOrder po) {
        if (auth != null) {
            po.setCreatedBy(auth.getName());
        }
        return ResponseEntity.ok(poService.createPO(po));
    }

    @PutMapping("/{id}/submit")
    @Operation(summary = "Submit a PO for approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<PurchaseOrder> submitPO(@PathVariable Long id) {
        return ResponseEntity.ok(poService.submitPO(id));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a pending PO")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PurchaseOrder> approvePO(@PathVariable Long id) {
        return ResponseEntity.ok(poService.approvePO(id));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a pending PO with reason")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PurchaseOrder> rejectPO(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(poService.rejectPO(id, reason));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Record goods receipt (triggers stock update)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<PurchaseOrder> receiveGoods(@PathVariable Long id,
                                                     @RequestParam Long productId,
                                                     @RequestParam Integer receivedQty) {
        return ResponseEntity.ok(poService.receiveGoods(id, productId, receivedQty));
    }

    @GetMapping
    @Operation(summary = "Get filtered list of Purchase Orders — scoped by warehouse for MANAGER/OFFICER")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER', 'STAFF')")
    public ResponseEntity<List<PurchaseOrder>> getPOs(
            Authentication auth,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        //  Warehouse scoping: only MANAGER is scoped to their own warehouse
        // OFFICER is global (can see all warehouses per case study §2.4)
        String role = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst().orElse("VIEWER");

        Long scopedWarehouseId = warehouseId; // default: use whatever was passed in

        if ("MANAGER".equals(role) || "STAFF".equals(role)) {
            // department is stored as String details by JwtAuthenticationFilter
            String department = auth.getDetails() instanceof String ? (String) auth.getDetails() : null;
            if (department != null && !department.isBlank()) {
                // Resolve department name → warehouseId via service
                scopedWarehouseId = poService.resolveWarehouseId(department);
            }
            if (scopedWarehouseId == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
        }
        // ADMIN & OFFICER pass through — no scoping enforced

        return ResponseEntity.ok(poService.getPOs(supplierId, scopedWarehouseId, status, startDate, endDate));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an existing PO")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<PurchaseOrder> cancelPO(@PathVariable Long id, @RequestParam String reason) {
        return ResponseEntity.ok(poService.cancelPO(id, reason));
    }

    // Called by alert-service scheduler to detect overdue POs
    @GetMapping("/overdue")
    @Operation(summary = "Get all overdue POs (approved but past expected delivery date)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OFFICER')")
    public ResponseEntity<List<PurchaseOrder>> getOverduePOs() {
        List<PurchaseOrder> overdue = poService.getPOs(null, null, "APPROVED", null, null)
                .stream()
                .filter(po -> po.getExpectedDeliveryDate() != null
                        && po.getExpectedDeliveryDate().isBefore(LocalDateTime.now()))
                .toList();
        return ResponseEntity.ok(overdue);
    }
}
