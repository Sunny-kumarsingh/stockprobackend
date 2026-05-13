package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.StockLevel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    // 🔥 Core query (MOST USED)
    Optional<StockLevel> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    // 📦 All stock in a warehouse
    List<StockLevel> findByWarehouseId(Long warehouseId);

    // 📦 Product across all warehouses
    List<StockLevel> findByProductId(Long productId);
    


    @Query("SELECT s FROM StockLevel s WHERE s.quantity <= s.minThreshold")
    List<StockLevel> findLowStockItems();
    
    
}