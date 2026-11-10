package com.lab.resource.repository;
import com.lab.resource.entity.Asset;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends CrudMapper<Asset> {
    default List<Asset> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).orderByDesc("created_at"));
    }

    default IPage<Asset> search(String query, String status, Long categoryId, Page<Asset> page) {
        QueryWrapper<Asset> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (status != null && !status.isBlank()) wrapper.eq("status", status);
        if (categoryId != null) wrapper.eq("category_id", categoryId);
        if (query != null && !query.isBlank()) {
            wrapper.and(condition -> condition.like("name", query)
                    .or().like("asset_no", query)
                    .or().like("serial_no", query));
        }
        wrapper.orderByDesc("created_at");
        return selectPage(page, wrapper);
    }

    default Optional<Asset> findByIdForUpdate(Long id) {
        return Optional.ofNullable(selectOne(Wrappers.<Asset>query().eq("id", id).last("FOR UPDATE")));
    }
}
