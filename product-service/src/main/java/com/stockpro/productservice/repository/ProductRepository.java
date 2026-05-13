package com.stockpro.productservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.stockpro.productservice.entity.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 🔍 Find by SKU (unique + active)
    Optional<Product> findBySkuAndIsActiveTrue(String sku);

    // 🔍 Find by ID (active only)
    Optional<Product> findByProductIdAndIsActiveTrue(Long productId);

    // 🔍 Filter by category (active only)
    List<Product> findByCategoryAndIsActiveTrue(String category);

    // 🔍 Filter by brand (active only)
    List<Product> findByBrandAndIsActiveTrue(String brand);

    // 🔍 Search by name (active only)
    List<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    // 🔍 Get all active products (paginated)
    Page<Product> findByIsActiveTrue(Pageable pageable);

    // 🔍 Get all active products (no pagination — for internal Feign calls)
    List<Product> findByIsActiveTrue();

    // 🔍 Barcode lookup (active only)
    Optional<Product> findByBarcodeAndIsActiveTrue(String barcode);

    // 📊 Count products by category (active only)
    Long countByCategoryAndIsActiveTrue(String category);

    // 🔥 Optional (admin/debug)
    List<Product> findByIsActive(Boolean isActive);
}