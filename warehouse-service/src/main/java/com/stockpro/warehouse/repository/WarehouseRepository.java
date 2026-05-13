package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    // 🔍 Get only active warehouses
    List<Warehouse> findByIsActive(Boolean isActive);

    // 🔍 Search by location
    List<Warehouse> findByLocation(String location);

    // 🔍 Get warehouses by manager
    List<Warehouse> findByManagerId(Long managerId);

    // 🔍 Find by name
    Optional<Warehouse> findByName(String name);
}