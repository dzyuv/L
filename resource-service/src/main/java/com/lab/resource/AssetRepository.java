package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface AssetRepository extends CrudMapper<Asset> {
    default List<Asset> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<Asset>query().eq("deleted", false).orderByDesc("created_at"));
    }
}
