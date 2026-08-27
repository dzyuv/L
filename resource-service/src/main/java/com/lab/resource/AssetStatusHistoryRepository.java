package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssetStatusHistoryRepository extends JpaRepository<AssetStatusHistory, Long> {
    List<AssetStatusHistory> findByAssetIdOrderByCreatedAtDesc(Long assetId);
}
