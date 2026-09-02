package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface AssetPurchaseRepository extends CrudMapper<AssetPurchase> {
    default List<AssetPurchase> findAllByOrderByPurchasedAtDesc() {
        return selectList(Wrappers.<AssetPurchase>query().orderByDesc("purchased_at").orderByDesc("id"));
    }
}
