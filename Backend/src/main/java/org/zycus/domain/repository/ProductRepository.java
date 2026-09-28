package org.zycus.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zycus.domain.model.Category;
import org.zycus.domain.model.Product;
import org.zycus.domain.model.ProductStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategory(Category category);

    List<Product> findByStatusAndCategory(ProductStatus status, Category category);

    Optional<Product> findBySku(String sku);

    @Query("SELECT COALESCE(AVG(p.demandVelocity), 0.0) FROM Product p WHERE p.category = :category")
    Double findAverageDemandVelocityByCategory(@Param("category") Category category);

    @Query("SELECT p FROM Product p WHERE p.stockLevel < p.reorderThreshold")
    List<Product> findLowStockProducts();
}
