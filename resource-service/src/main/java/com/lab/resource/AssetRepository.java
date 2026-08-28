package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface AssetRepository extends CrudMapper<Asset> {
    default List<Asset> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).orderByDesc("created_at"));
    }
    default List<Asset> findByDeletedFalseAndStatusOrderByCreatedAtDesc(String status) {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).eq("status", status).orderByDesc("created_at"));
    }
    default List<Asset> findByDeletedFalseAndCategoryIdOrderByCreatedAtDesc(Long categoryId) {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).eq("category_id", categoryId).orderByDesc("created_at"));
    }
    default List<Asset> findByDeletedFalseAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name) {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).like("name", name).orderByDesc("created_at"));
    }
}
