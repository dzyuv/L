package com.lab.resource.repository;
import com.lab.resource.entity.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface AssetStatusHistoryRepository extends CrudMapper<AssetStatusHistory> {
    default List<AssetStatusHistory> findByAssetIdOrderByCreatedAtDesc(Long assetId) {
        return selectList(Wrappers.<AssetStatusHistory>query().eq("asset_id", assetId).orderByDesc("created_at"));
    }
}
