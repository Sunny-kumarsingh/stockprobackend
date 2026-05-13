package com.stockpro.productservice.service.impl;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;
import com.stockpro.productservice.entity.Product;
import com.stockpro.productservice.repository.ProductRepository;
import com.stockpro.productservice.service.ProductService;
import com.stockpro.productservice.exception.ResourceNotFoundException;
import com.stockpro.productservice.mapper.ProductMapper;

import java.util.List;

import static com.stockpro.productservice.mapper.ProductMapper.*;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final String PRODUCT_NOT_FOUND = "Product not found: ";

    private final ProductRepository repository;
    private final com.stockpro.productservice.client.WarehouseClient warehouseClient;

    //  CREATE
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public ProductResponseDTO createProduct(ProductRequestDTO dto) {

        Product product = toEntity(dto);

        repository.findBySkuAndIsActiveTrue(product.getSku())
                .ifPresent(p -> {
                    throw new RuntimeException("SKU already exists");
                });

        return toDTO(repository.save(product));
    }

    //  GET BY ID
    @Override
    @Cacheable(value = "product", key = "#p0")
    public ProductResponseDTO getById(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));
        return toDTO(product);
    }

    //  GET BY SKU
    @Override
    @Cacheable(value = "productBySku", key = "#p0")
    public ProductResponseDTO getBySku(String sku) {
        Product product = repository.findBySkuAndIsActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + sku));
        return toDTO(product);
    }

    //  GET ALL (paginated)
    @Override
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        return repository.findByIsActiveTrue(pageable)
                .map(ProductMapper::toDTO);
    }

    //  GET ALL — flat list for internal Feign calls (no pagination)
    @Override
    @Cacheable(value = "activeProducts")
    public List<ProductResponseDTO> getAllActiveProducts() {
        return repository.findByIsActiveTrue()
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    //  GET BY CATEGORY
    @Override
    @Cacheable(value = "productsByCategory", key = "#p0")
    public List<ProductResponseDTO> getByCategory(String category) {
        return repository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    //  GET BY BRAND
    @Override
    @Cacheable(value = "productsByBrand", key = "#p0")
    public List<ProductResponseDTO> getByBrand(String brand) {
        return repository.findByBrandAndIsActiveTrue(brand)
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    //  SEARCH
    @Override
    @Cacheable(value = "productSearch", key = "#p0")
    public List<ProductResponseDTO> searchProducts(String name) {
        return repository.findByNameContainingIgnoreCaseAndIsActiveTrue(name)
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    //  UPDATE
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto) {

        Product existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));

        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setCategory(dto.getCategory());
        existing.setBrand(dto.getBrand());
        existing.setUnitOfMeasure(dto.getUnitOfMeasure());
        existing.setCostPrice(dto.getCostPrice());
        existing.setSellingPrice(dto.getSellingPrice());
        existing.setReorderLevel(dto.getReorderLevel());
        existing.setMaxStockLevel(dto.getMaxStockLevel());
        existing.setLeadTimeDays(dto.getLeadTimeDays());
        existing.setBarcode(dto.getBarcode());
        existing.setImageUrl(dto.getImageUrl());

        return toDTO(repository.save(existing));
    }

    //  SOFT DELETE
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public void deactivateProduct(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));

        product.setIsActive(false);
        repository.save(product);
    }

    //  HARD DELETE
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public void deleteProduct(Long id) {
        repository.deleteById(id);
    }

    //  GET BY BARCODE
    @Override
    @Cacheable(value = "productByBarcode", key = "#p0")
    public ProductResponseDTO getByBarcode(String barcode) {
        Product product = repository.findByBarcodeAndIsActiveTrue(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + barcode));

        return toDTO(product);
    }

    //  Feign: Update totalStock called by Warehouse Service
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public void updateTotalStock(Long productId, Integer total) {
        Product product = repository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + productId));
        product.setTotalStock(total);
        repository.save(product);
        log.info("[Feign] Stock synced for Product {} -> {} units", productId, total);
    }

    //  Get all deactivated products
    @Override
    @Cacheable(value = "inactiveProducts")
    public List<ProductResponseDTO> getInactiveProducts() {
        return repository.findByIsActive(false)
                .stream()
                .map(ProductMapper::toDTO)
                .toList();
    }

    //  Reactivate a deactivated product
    @Override
    @CacheEvict(value = {
            "product", "productBySku", "productByBarcode", "activeProducts",
            "inactiveProducts", "productsByCategory", "productsByBrand", "productSearch"
    }, allEntries = true)
    public void activateProduct(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND + id));
        product.setIsActive(true);
        repository.save(product);
    }
}
