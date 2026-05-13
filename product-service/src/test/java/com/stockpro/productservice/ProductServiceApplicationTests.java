package com.stockpro.productservice;

import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;
import com.stockpro.productservice.entity.Product;
import com.stockpro.productservice.exception.ResourceNotFoundException;
import com.stockpro.productservice.repository.ProductRepository;
import com.stockpro.productservice.service.impl.ProductServiceImpl;
import com.stockpro.productservice.client.WarehouseClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for ProductServiceImpl.
 * Tests: create, getById, getBySku, update, deactivate, reactivate, search, stockSync.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl - Unit Tests")
class ProductServiceApplicationTests {

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
				.category("Electronics")
				.brand("Dell")
				.unitOfMeasure("piece")
				.costPrice(45000.0)
				.sellingPrice(55000.0)
				.reorderLevel(5)
				.maxStockLevel(100)
				.leadTimeDays(7)
				.barcode("123456789")
				.isActive(true)
				.totalStock(20)
				.build();

		requestDTO = new ProductRequestDTO();
		requestDTO.setSku("PROD-001");
		requestDTO.setName("Laptop");
		requestDTO.setCategory("Electronics");
		requestDTO.setBrand("Dell");
		requestDTO.setCostPrice(45000.0);
		requestDTO.setSellingPrice(55000.0);
		requestDTO.setReorderLevel(5);
		requestDTO.setMaxStockLevel(100);
		requestDTO.setLeadTimeDays(7);
		requestDTO.setBarcode("123456789");
		requestDTO.setUnitOfMeasure("piece");
	}

	@Test
	@DisplayName("createProduct() - should save and return product when SKU is unique")
	void createProduct_ShouldReturnDTO_WhenSkuIsUnique() {
		when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.empty());
		when(repository.save(any(Product.class))).thenReturn(testProduct);

		ProductResponseDTO result = productService.createProduct(requestDTO);

		assertThat(result).isNotNull();
		assertThat(result.getSku()).isEqualTo("PROD-001");
		assertThat(result.getName()).isEqualTo("Laptop");
		verify(repository).save(any(Product.class));
	}

	@Test
	@DisplayName("createProduct() - should throw when SKU already exists")
	void createProduct_ShouldThrow_WhenSkuAlreadyExists() {
		when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.of(testProduct));

		assertThatThrownBy(() -> productService.createProduct(requestDTO))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("SKU already exists");

		verify(repository, never()).save(any());
	}

	@Test
	@DisplayName("getById() - should return product when found")
	void getById_ShouldReturnProduct_WhenExists() {
		when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

		ProductResponseDTO result = productService.getById(1L);

		assertThat(result).isNotNull();
		assertThat(result.getProductId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("getById() - should throw ResourceNotFoundException when not found")
	void getById_ShouldThrow_WhenProductNotFound() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getById(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Product not found");
	}

	@Test
	@DisplayName("getBySku() - should return product when SKU exists")
	void getBySku_ShouldReturnProduct_WhenExists() {
		when(repository.findBySkuAndIsActiveTrue("PROD-001")).thenReturn(Optional.of(testProduct));

		ProductResponseDTO result = productService.getBySku("PROD-001");

		assertThat(result.getSku()).isEqualTo("PROD-001");
	}

	@Test
	@DisplayName("getBySku() - should throw when SKU not found")
	void getBySku_ShouldThrow_WhenSkuNotFound() {
		when(repository.findBySkuAndIsActiveTrue("INVALID")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getBySku("INVALID"))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("updateProduct() - should update and save product")
	void updateProduct_ShouldUpdateFields_WhenProductExists() {
		when(repository.findById(1L)).thenReturn(Optional.of(testProduct));
		when(repository.save(any(Product.class))).thenReturn(testProduct);

		requestDTO.setName("Updated Laptop");
		ProductResponseDTO result = productService.updateProduct(1L, requestDTO);

		assertThat(result).isNotNull();
		verify(repository).save(any(Product.class));
	}

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
	@DisplayName("deactivateProduct() - should throw when product not found")
	void deactivateProduct_ShouldThrow_WhenProductNotFound() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.deactivateProduct(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

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
	@DisplayName("searchProducts() - should return matching products")
	void searchProducts_ShouldReturnResults_WhenMatchFound() {
		when(repository.findByNameContainingIgnoreCaseAndIsActiveTrue("Laptop"))
				.thenReturn(List.of(testProduct));

		List<ProductResponseDTO> result = productService.searchProducts("Laptop");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Laptop");
	}

	@Test
	@DisplayName("searchProducts() - should return empty when no match")
	void searchProducts_ShouldReturnEmpty_WhenNoMatch() {
		when(repository.findByNameContainingIgnoreCaseAndIsActiveTrue("xyz"))
				.thenReturn(List.of());

		List<ProductResponseDTO> result = productService.searchProducts("xyz");

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("updateTotalStock() - should sync stock count on product")
	void updateTotalStock_ShouldSetTotalStock_WhenProductFound() {
		when(repository.findById(1L)).thenReturn(Optional.of(testProduct));

		productService.updateTotalStock(1L, 50);

		assertThat(testProduct.getTotalStock()).isEqualTo(50);
		verify(repository).save(testProduct);
	}
}
