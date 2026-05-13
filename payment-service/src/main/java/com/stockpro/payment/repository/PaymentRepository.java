package com.stockpro.payment.repository;

import com.stockpro.payment.entity.Payment;
import com.stockpro.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPurchaseOrderId(Long purchaseOrderId);

    boolean existsByPurchaseOrderId(Long purchaseOrderId);

    List<Payment> findByWarehouseId(Long warehouseId);

    List<Payment> findBySupplierId(Long supplierId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByRequestedBy(String requestedBy);
}
