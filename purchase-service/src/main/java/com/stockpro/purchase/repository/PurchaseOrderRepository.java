package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.entity.POStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    //  Filter by supplier
    List<PurchaseOrder> findBySupplierId(Long supplierId);

    //  Filter by warehouse
    List<PurchaseOrder> findByWarehouseId(Long warehouseId);

    //  Filter by status
    List<PurchaseOrder> findByStatus(POStatus status);

    //  Filter by date range
    List<PurchaseOrder> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    //  Combined filters (important for dashboard)
    List<PurchaseOrder> findBySupplierIdAndStatus(Long supplierId, POStatus status);

    List<PurchaseOrder> findByWarehouseIdAndStatus(Long warehouseId, POStatus status);

    //  Created by user (Purchase Officer view)
    List<PurchaseOrder> findByCreatedById(Long userId);

    //  Pending approvals (Manager view)
    List<PurchaseOrder> findByStatusOrderByOrderDateDesc(POStatus status);

    //  Overdue Check (Scheduler use)
    List<PurchaseOrder> findByStatusInAndExpectedDeliveryDateBefore(List<POStatus> statuses, LocalDateTime now);
}