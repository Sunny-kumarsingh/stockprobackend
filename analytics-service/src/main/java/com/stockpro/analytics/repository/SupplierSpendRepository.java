package com.stockpro.analytics.repository;

import com.stockpro.analytics.entity.SupplierSpend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierSpendRepository extends JpaRepository<SupplierSpend, Long> {

    // Top suppliers by spend (for supplier analytics dashboard)
    @Query("SELECT s FROM SupplierSpend s ORDER BY s.totalSpend DESC")
    List<SupplierSpend> findTopSuppliersBySpend();
}
