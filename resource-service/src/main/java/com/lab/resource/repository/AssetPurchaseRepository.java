package com.lab.resource.repository;
import com.lab.resource.entity.AssetPurchase;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;

public interface AssetPurchaseRepository extends CrudMapper<AssetPurchase> {
    default IPage<AssetPurchase> search(Page<AssetPurchase> page) {
        return selectPage(page, Wrappers.<AssetPurchase>query().orderByDesc("purchased_at").orderByDesc("id"));
    }
}
