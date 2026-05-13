package com.stockpro.purchase.service;

import com.stockpro.purchase.entity.PurchaseOrder;

import java.time.LocalDateTime;
import java.util.List;

public interface PurchaseOrderService {

    PurchaseOrder createPO(PurchaseOrder po);

    PurchaseOrder submitPO(Long id);

    PurchaseOrder approvePO(Long id);

    PurchaseOrder rejectPO(Long id, String reason);

    PurchaseOrder receiveGoods(Long id, Long productId, Integer receivedQty);

    PurchaseOrder cancelPO(Long id, String reason);

    List<PurchaseOrder> getPOs(Long supplierId,
                               Long warehouseId,
                               String status,
                               LocalDateTime startDate,
                               LocalDateTime endDate);

    // Resolves a warehouse name (department) to its warehouseId — used for MANAGER/OFFICER scoping
    Long resolveWarehouseId(String warehouseName);
}