package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends CrudMapper<Asset> {
    default List<Asset> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).orderByDesc("created_at"));
    }

    default Optional<Asset> findByIdForUpdate(Long id) {
        return Optional.ofNullable(selectOne(Wrappers.<Asset>query().eq("id", id).last("FOR UPDATE")));
    }
}
