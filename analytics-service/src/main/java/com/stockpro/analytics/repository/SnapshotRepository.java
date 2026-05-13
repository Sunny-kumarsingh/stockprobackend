// SnapshotRepository.java
package com.stockpro.analytics.repository;

import com.stockpro.analytics.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SnapshotRepository extends JpaRepository<InventorySnapshot, Long> {

    List<InventorySnapshot> findByProductId(Long productId);

    List<InventorySnapshot> findByWarehouseId(Long warehouseId);

    List<InventorySnapshot> findBySnapshotDateBetween(LocalDateTime start, LocalDateTime end);
}
