package com.stockpro.productservice;

import com.stockpro.productservice.contoller.ProductResource;
import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;
import com.stockpro.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductResourceTest {

    @Mock
    private ProductService service;

    private ProductResource resource;
    private ProductRequestDTO request;
    private ProductResponseDTO response;

    @BeforeEach
    void setUp() {
        resource = new ProductResource(service);
        request = new ProductRequestDTO();
        request.setSku("SKU-1");
        request.setName("Keyboard");

        response = ProductResponseDTO.builder()
                .productId(1L)
                .sku("SKU-1")
                .name("Keyboard")
                .category("Electronics")
                .brand("Logi")
                .barcode("BAR-1")
                .isActive(true)
                .totalStock(12)
                .build();
    }

    @Test
    void createDelegatesToService() {
        when(service.createProduct(request)).thenReturn(response);

        ProductResponseDTO result = resource.create(request);

        assertThat(result.getSku()).isEqualTo("SKU-1");
        verify(service).createProduct(request);
    }

    @Test
    void readEndpointsDelegateToService() {
        Page<ProductResponseDTO> page = new PageImpl<>(List.of(response));
        when(service.getById(1L)).thenReturn(response);
        when(service.getBySku("SKU-1")).thenReturn(response);
        when(service.getByBarcode("BAR-1")).thenReturn(response);
        when(service.getAllProducts(any())).thenReturn(page);
        when(service.getAllActiveProducts()).thenReturn(List.of(response));

        assertThat(resource.getById(1L).getProductId()).isEqualTo(1L);
        assertThat(resource.getBySku("SKU-1").getSku()).isEqualTo("SKU-1");
        assertThat(resource.getByBarcode("BAR-1").getBarcode()).isEqualTo("BAR-1");
        assertThat(resource.getAll(PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        assertThat(resource.getAllInternal()).hasSize(1);
    }

    @Test
    void filterEndpointsDelegateToService() {
        when(service.getByCategory("Electronics")).thenReturn(List.of(response));
        when(service.getByBrand("Logi")).thenReturn(List.of(response));
        when(service.searchProducts("key")).thenReturn(List.of(response));

        assertThat(resource.getByCategory("Electronics")).hasSize(1);
        assertThat(resource.getByBrand("Logi")).hasSize(1);
        assertThat(resource.search("key")).hasSize(1);
    }

    @Test
    void mutationEndpointsReturnMessagesAndDelegate() {
        when(service.updateProduct(1L, request)).thenReturn(response);
        when(service.getInactiveProducts()).thenReturn(List.of(response));

        assertThat(resource.updateProduct(1L, request).getName()).isEqualTo("Keyboard");
        assertThat(resource.deactivateProduct(1L)).contains("deactivated");
        assertThat(resource.activateProduct(1L)).contains("reactivated");
        assertThat(resource.deleteProduct(1L)).contains("deleted");
        assertThat(resource.getInactiveProducts()).hasSize(1);

        verify(service).deactivateProduct(1L);
        verify(service).activateProduct(1L);
        verify(service).deleteProduct(1L);
    }

    @Test
    void validationAndStockEndpointsDelegate() {
        resource.validateProduct(1L);
        resource.validateProductBySku("SKU-1");
        resource.updateTotalStock(1L, 25);

        verify(service).getById(1L);
        verify(service).getBySku("SKU-1");
        verify(service).updateTotalStock(1L, 25);
    }
}
