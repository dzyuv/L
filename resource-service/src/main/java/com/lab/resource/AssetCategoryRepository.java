package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    List<AssetCategory> findByEnabledTrueOrderByNameAsc();
    Optional<AssetCategory> findByNameIgnoreCase(String name);
}
