package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByDeletedFalseOrderByCreatedAtDesc();
    List<Asset> findByDeletedFalseAndStatusOrderByCreatedAtDesc(String status);
    List<Asset> findByDeletedFalseAndCategoryIdOrderByCreatedAtDesc(Long categoryId);
    List<Asset> findByDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
}
