package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Optional;

public interface AssetCategoryRepository extends CrudMapper<AssetCategory> {
    default List<AssetCategory> findByEnabledTrueOrderByNameAsc() {
        return selectList(Wrappers.<AssetCategory>query().eq("enabled", true).orderByAsc("name"));
    }
    default Optional<AssetCategory> findByNameIgnoreCase(String name) {
        return Optional.ofNullable(selectOne(Wrappers.<AssetCategory>query().apply("LOWER(name)=LOWER({0})", name)));
    }
}
