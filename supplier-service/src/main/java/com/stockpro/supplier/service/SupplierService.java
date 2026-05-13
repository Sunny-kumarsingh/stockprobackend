package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;
import java.util.List;

public interface SupplierService {

    Supplier createSupplier(Supplier supplier);

    Supplier getById(Long supplierId);

    List<Supplier> getAllSuppliers();

    List<Supplier> searchSuppliers(String query);

    Supplier updateSupplier(Long supplierId, Supplier supplier);

    void deactivateSupplier(Long supplierId);

    void deleteSupplier(Long supplierId);

    List<Supplier> getByCity(String city);

    List<Supplier> getByCountry(String country);

    Supplier updateRating(Long supplierId, double rating);
}