package com.stockpro.productservice.service;

import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    //  Create
    ProductResponseDTO createProduct(ProductRequestDTO dto);

    //  Read
    ProductResponseDTO getById(Long id);
    ProductResponseDTO getBySku(String sku);
    ProductResponseDTO getByBarcode(String barcode);

    //  List / Filters
    Page<ProductResponseDTO> getAllProducts(Pageable pageable);
    List<ProductResponseDTO> getAllActiveProducts(); // Internal — no pagination
    List<ProductResponseDTO> getByCategory(String category);
    List<ProductResponseDTO> getByBrand(String brand);
    List<ProductResponseDTO> searchProducts(String name);

    //  Update
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto);

    //  Soft Delete (preferred)
    void deactivateProduct(Long id);

    //  Hard Delete (admin/debug only)
    void deleteProduct(Long id);

    //  Feign: called by Warehouse Service to sync live stock
    void updateTotalStock(Long productId, Integer total);

    //  Get all deactivated products (Admin)
    List<ProductResponseDTO> getInactiveProducts();

    //  Reactivate a deactivated product
    void activateProduct(Long id);
}