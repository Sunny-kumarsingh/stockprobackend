package com.stockpro.productservice.contoller;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.stockpro.productservice.dto.ProductRequestDTO;
import com.stockpro.productservice.dto.ProductResponseDTO;
import com.stockpro.productservice.service.ProductService;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductResource {

    private final ProductService service;

    @Value("${stockpro.upload.product-dir:uploads/products}")
    private String productUploadDir;

    //  CREATE
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public ProductResponseDTO create(@Valid @RequestBody ProductRequestDTO dto) {
        return service.createProduct(dto);
    }

    //  UPLOAD IMAGE
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        // Sanitize filename — replace spaces with underscores to ensure valid URLs
        String rawName = file.getOriginalFilename();
        String originalName = (rawName != null) ? rawName.replaceAll("\\s+", "_") : "image";
        String fileName = System.currentTimeMillis() + "_" + originalName;
        Path path = Paths.get(productUploadDir).resolve(fileName).toAbsolutePath().normalize();

        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        return "/uploads/products/" + fileName;
    }

    //  GET by ID
    @GetMapping("/{id}")
    public ProductResponseDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }

    //  GET by SKU
    @GetMapping("/sku/{sku}")
    public ProductResponseDTO getBySku(@PathVariable String sku) {
        return service.getBySku(sku);
    }

    //  CHECK if exists
    @GetMapping("/{id}/exists")
    public void validateProduct(@PathVariable Long id) {
        // If the product doesn't exist, service.getById will throw an exception.
        // Otherwise, it succeeds and returns 200 OK.
        service.getById(id);
    }

    //  CHECK if exists by SKU
    @GetMapping("/sku/{sku}/exists")
    public void validateProductBySku(@PathVariable String sku) {
        service.getBySku(sku);
    }

    //  GET ALL (paginated — for UI)
    @GetMapping
    public Page<ProductResponseDTO> getAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        return service.getAllProducts(pageable);
    }

    //  GET ALL — flat list (for internal Feign clients, no pagination)
    @GetMapping("/all")
    public List<ProductResponseDTO> getAllInternal() {
        return service.getAllActiveProducts();
    }

    //  FILTER by CATEGORY
    @GetMapping("/category/{category}")
    public List<ProductResponseDTO> getByCategory(@PathVariable String category) {
        return service.getByCategory(category);
    }

    //  FILTER by BRAND
    @GetMapping("/brand/{brand}")
    public List<ProductResponseDTO> getByBrand(@PathVariable String brand) {
        return service.getByBrand(brand);
    }

    //  SEARCH
    @GetMapping("/search")
    public List<ProductResponseDTO> search(@RequestParam String name) {
        return service.searchProducts(name);
    }

    //  BARCODE LOOKUP
    @GetMapping("/barcode/{barcode}")
    public ProductResponseDTO getByBarcode(@PathVariable String barcode) {
        return service.getByBarcode(barcode);
    }

    //  REMOVED LOW STOCK (belongs to Warehouse Service)

    //  UPDATE
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody ProductRequestDTO dto) {
        return service.updateProduct(id, dto);
    }

    //  SOFT DELETE
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/deactivate/{id}")
    public String deactivateProduct(@PathVariable Long id) {
        service.deactivateProduct(id);
        return "Product deactivated successfully";
    }

    //  HARD DELETE
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        service.deleteProduct(id);
        return "Product deleted successfully";
    }

    //  Internal Feign endpoint: Warehouse Service syncs total stock
    @PutMapping("/{id}/stock")
    public void updateTotalStock(@PathVariable Long id, @RequestParam("total") Integer total) {
        service.updateTotalStock(id, total);
    }

    //  Get all deactivated products (Admin/Manager)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @GetMapping("/inactive")
    public List<ProductResponseDTO> getInactiveProducts() {
        return service.getInactiveProducts();
    }

    //  Reactivate a deactivated product (Admin/Manager)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}/activate")
    public String activateProduct(@PathVariable Long id) {
        service.activateProduct(id);
        return "Product reactivated successfully";
    }
}
