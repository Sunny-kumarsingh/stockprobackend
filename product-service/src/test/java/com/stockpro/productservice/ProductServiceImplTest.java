package com.stockpro.productservice;

import com.stockpro.productservice.client.WarehouseClient;
import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;
import com.stockpro.productservice.entity.Product;
import com.stockpro.productservice.exception.ResourceNotFoundException;
import com.stockpro.productservice.repository.ProductRepository;
import com.stockpro.productservice.service.impl.ProductServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceImpl using JUnit 5 + Mockito.
 * Tests: create, getById, getBySku, update, deactivate, reactivate, delete, search,
 *        getByCategory, getByBrand, getByBarcode, getAllActive, getAllPaginated,
 *        getInactive, updateTotalStock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl - Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository repository;

    @Mock
    private WarehouseClient warehouseClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .productId(1L)
                .sku("PROD-001")
                .name("Laptop")
                .description("High-performance laptop")
                .category("Electronics")
                .brand("Dell")
                .unitOfMeasure("piece")
                .costPrice(45000.0)
                .sellingPrice(55000.0)
                .reorderLevel(5)
                .maxStockLevel(100)
                .leadTimeDays(7)
                .barcode("123456789")
                .imageUrl("/uploads/products/laptop.jpg")
                .isActive(true)
                .totalStock(20)
                .build();

        requestDTO = new ProductRequestDTO();
        requestDTO.setSku("PROD-001");
        requestDTO.setName("Laptop");
        requestDTO.setDescription("High-performance laptop");
        requestDTO.setCategory("Electronics");
        requestDTO.setBrand("Dell");
        requestDTO.setCostPrice(45000.0);
        requestDTO.setSellingPrice(55000.0);
        requestDTO.setReorderLevel(5);
        requestDTO.setMaxStockLevel(100);
        requestDTO.setLeadTimeDays(7);
        requestDTO.setBarcode("123456789");
        requestDTO.setUnitOfMeasure("piece");
        requestDTO.setImageUrl("/uploads/products/laptop.jpg");
    }

    // ─────────────────────────────────────────────
    // CREATE PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("createProduct() - should create and return new product")
    void createProduct_ShouldReturnDTO_WhenSkuIsUnique() {
        when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.empty());
        when(repository.save(any(Product.class))).thenReturn(testProduct);

        ProductResponseDTO result = productService.createProduct(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getSku()).isEqualTo("PROD-001");
        assertThat(result.getImageUrl()).isEqualTo("/uploads/products/laptop.jpg");
        assertThat(result.getDescription()).isEqualTo("High-performance laptop");
        verify(repository).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct() - should throw RuntimeException if SKU already exists")
    void createProduct_ShouldThrow_WhenSkuAlreadyExists() {
        when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.createProduct(requestDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SKU already exists");

        verify(repository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    // GET BY ID TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById() - should return product when found")
    void getById_ShouldReturnProduct_WhenExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductResponseDTO result = productService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(1L);
        assertThat(result.getBrand()).isEqualTo("Dell");
    }

    @Test
    @DisplayName("getById() - should throw ResourceNotFoundException when not found")
    void getById_ShouldThrow_WhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    // ─────────────────────────────────────────────
    // GET BY SKU TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getBySku() - should return product when found")
    void getBySku_ShouldReturnProduct_WhenExists() {
        when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.of(testProduct));

        ProductResponseDTO result = productService.getBySku("PROD-001");

        assertThat(result.getSku()).isEqualTo("PROD-001");
    }

    @Test
    @DisplayName("getBySku() - should throw ResourceNotFoundException when not found")
    void getBySku_ShouldThrow_WhenSkuNotFound() {
        when(repository.findBySkuAndIsActiveTrue("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySku("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────
    // GET BY BARCODE TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByBarcode() - should return product when barcode found")
    void getByBarcode_ShouldReturnProduct_WhenExists() {
        when(repository.findByBarcodeAndIsActiveTrue("123456789")).thenReturn(Optional.of(testProduct));

        ProductResponseDTO result = productService.getByBarcode("123456789");

        assertThat(result).isNotNull();
        assertThat(result.getBarcode()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("getByBarcode() - should throw ResourceNotFoundException when barcode not found")
    void getByBarcode_ShouldThrow_WhenBarcodeNotFound() {
        when(repository.findByBarcodeAndIsActiveTrue("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getByBarcode("999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    // ─────────────────────────────────────────────
    // GET ALL (Paginated) TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllProducts() - should return paginated active products")
    void getAllProducts_ShouldReturnPage_WhenCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(repository.findByIsActiveTrue(pageable)).thenReturn(productPage);

        Page<ProductResponseDTO> result = productService.getAllProducts(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("getAllProducts() - should return empty page when no products")
    void getAllProducts_ShouldReturnEmptyPage_WhenNoProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByIsActiveTrue(pageable)).thenReturn(Page.empty());

        Page<ProductResponseDTO> result = productService.getAllProducts(pageable);

        assertThat(result.getTotalElements()).isZero();
    }

    // ─────────────────────────────────────────────
    // GET ALL ACTIVE (flat list for Feign)
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getAllActiveProducts() - should return all active products as list")
    void getAllActiveProducts_ShouldReturnList_WhenProductsExist() {
        when(repository.findByIsActiveTrue()).thenReturn(List.of(testProduct));

        List<ProductResponseDTO> result = productService.getAllActiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("getAllActiveProducts() - should return empty list when none active")
    void getAllActiveProducts_ShouldReturnEmpty_WhenNoneActive() {
        when(repository.findByIsActiveTrue()).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getAllActiveProducts();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET BY CATEGORY TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByCategory() - should return products in that category")
    void getByCategory_ShouldReturnProducts_WhenCategoryMatches() {
        when(repository.findByCategoryAndIsActiveTrue("Electronics")).thenReturn(List.of(testProduct));

        List<ProductResponseDTO> result = productService.getByCategory("Electronics");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("getByCategory() - should return empty list when category not found")
    void getByCategory_ShouldReturnEmpty_WhenCategoryNotFound() {
        when(repository.findByCategoryAndIsActiveTrue("Furniture")).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getByCategory("Furniture");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET BY BRAND TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getByBrand() - should return products matching brand")
    void getByBrand_ShouldReturnProducts_WhenBrandMatches() {
        when(repository.findByBrandAndIsActiveTrue("Dell")).thenReturn(List.of(testProduct));

        List<ProductResponseDTO> result = productService.getByBrand("Dell");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBrand()).isEqualTo("Dell");
    }

    @Test
    @DisplayName("getByBrand() - should return empty list when brand not found")
    void getByBrand_ShouldReturnEmpty_WhenBrandNotFound() {
        when(repository.findByBrandAndIsActiveTrue("UnknownBrand")).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getByBrand("UnknownBrand");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // GET INACTIVE PRODUCTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getInactiveProducts() - should return deactivated products")
    void getInactiveProducts_ShouldReturnInactiveList_WhenExists() {
        testProduct.setIsActive(false);
        when(repository.findByIsActive(false)).thenReturn(List.of(testProduct));

        List<ProductResponseDTO> result = productService.getInactiveProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isFalse();
    }

    @Test
    @DisplayName("getInactiveProducts() - should return empty if no deactivated products")
    void getInactiveProducts_ShouldReturnEmpty_WhenNoneInactive() {
        when(repository.findByIsActive(false)).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getInactiveProducts();

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct() - should update and return product")
    void updateProduct_ShouldUpdateFields_WhenProductExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(repository.save(any(Product.class))).thenReturn(testProduct);

        requestDTO.setName("Updated Laptop");
        requestDTO.setSellingPrice(60000.0);

        ProductResponseDTO result = productService.updateProduct(1L, requestDTO);

        assertThat(result).isNotNull();
        verify(repository).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct() - should throw ResourceNotFoundException when product not found")
    void updateProduct_ShouldThrow_WhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    // ─────────────────────────────────────────────
    // DEACTIVATE PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deactivateProduct() - should set isActive to false")
    void deactivateProduct_ShouldDeactivate_WhenProductExists() {
        testProduct.setIsActive(true);
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.deactivateProduct(1L);

        assertThat(testProduct.getIsActive()).isFalse();
        verify(repository).save(testProduct);
    }

    @Test
    @DisplayName("deactivateProduct() - should throw ResourceNotFoundException when not found")
    void deactivateProduct_ShouldThrow_WhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deactivateProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────
    // REACTIVATE PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("activateProduct() - should set isActive to true")
    void activateProduct_ShouldReactivate_WhenProductExists() {
        testProduct.setIsActive(false);
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.activateProduct(1L);

        assertThat(testProduct.getIsActive()).isTrue();
        verify(repository).save(testProduct);
    }

    @Test
    @DisplayName("activateProduct() - should throw ResourceNotFoundException when not found")
    void activateProduct_ShouldThrow_WhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.activateProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─────────────────────────────────────────────
    // DELETE PRODUCT TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("deleteProduct() - should call deleteById on repository")
    void deleteProduct_ShouldCallDeleteById_WhenInvoked() {
        productService.deleteProduct(1L);

        verify(repository).deleteById(1L);
    }

    // ─────────────────────────────────────────────
    // SEARCH TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("searchProducts() - should return matching products")
    void searchProducts_ShouldReturnResults_WhenMatchFound() {
        when(repository.findByNameContainingIgnoreCaseAndIsActiveTrue("Laptop"))
                .thenReturn(List.of(testProduct));

        List<ProductResponseDTO> result = productService.searchProducts("Laptop");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    @DisplayName("searchProducts() - should return empty list when no match")
    void searchProducts_ShouldReturnEmpty_WhenNoMatch() {
        when(repository.findByNameContainingIgnoreCaseAndIsActiveTrue("unknown"))
                .thenReturn(List.of());

        List<ProductResponseDTO> result = productService.searchProducts("unknown");

        assertThat(result).isEmpty();
    }

    // ─────────────────────────────────────────────
    // UPDATE TOTAL STOCK (Feign) TESTS
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("updateTotalStock() - should update stock count on product")
    void updateTotalStock_ShouldSetTotalStock_WhenProductFound() {
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.updateTotalStock(1L, 50);

        assertThat(testProduct.getTotalStock()).isEqualTo(50);
        verify(repository).save(testProduct);
    }

    @Test
    @DisplayName("updateTotalStock() - should throw ResourceNotFoundException when product not found")
    void updateTotalStock_ShouldThrow_WhenProductNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateTotalStock(99L, 50))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateTotalStock() - should set stock to 0 when passing zero")
    void updateTotalStock_ShouldSetZero_WhenStockIsZero() {
        when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

        productService.updateTotalStock(1L, 0);

        assertThat(testProduct.getTotalStock()).isZero();
        verify(repository).save(testProduct);
    }
}
