package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface AssetStatusHistoryRepository extends CrudMapper<AssetStatusHistory> {
    default List<AssetStatusHistory> findByAssetIdOrderByCreatedAtDesc(Long assetId) {
        return selectList(Wrappers.<AssetStatusHistory>query().eq("asset_id", assetId).orderByDesc("created_at"));
    }
}
