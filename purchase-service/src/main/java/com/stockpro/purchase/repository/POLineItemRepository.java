package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.POLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POLineItemRepository extends JpaRepository<POLineItem, Long> {

    // Get all items of a PO
    List<POLineItem> findByPurchaseOrderId(Long purchaseOrderId);
}