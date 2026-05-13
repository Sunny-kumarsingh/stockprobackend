package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.StockTransferRequest;
import com.stockpro.warehouse.entity.TransferRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferRequestRepository extends JpaRepository<StockTransferRequest, Long> {
    List<StockTransferRequest> findByRequestingWarehouseIdOrderByRequestedAtDesc(Long requestingWarehouseId);
    List<StockTransferRequest> findByStatusOrderByRequestedAtDesc(TransferRequestStatus status);
}
