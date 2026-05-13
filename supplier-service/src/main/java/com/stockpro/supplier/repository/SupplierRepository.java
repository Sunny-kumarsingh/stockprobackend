package com.stockpro.supplier.repository;

import com.stockpro.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Supplier findBySupplierId(Long supplierId);

    List<Supplier> findByCity(String city);

    List<Supplier> findByCountry(String country);

    List<Supplier> findByIsActive(Boolean isActive);

    Optional<Supplier> findByTaxId(String taxId);

    // 📌 NOTE: PDF names this method 'searchByName()' but Spring Data JPA
    // cannot auto-derive a query for searching across MULTIPLE fields (name, city, country).
    // So we use a custom @Query here. The method name matches PDF exactly.
    // PDF §2.4: "Search suppliers by name, city, or country"
    @Query("SELECT s FROM Supplier s WHERE " +
           "LOWER(s.name)    LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(s.city)    LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(s.country) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Supplier> searchByName(@Param("name") String name);

    Long countByIsActive(Boolean isActive);

    // PDF names this 'deleteBySupplied()' — likely a typo for deleteBySupplierId()
    // Kept as deleteBySupplierId to match intent
    void deleteBySupplierId(Long supplierId);
}