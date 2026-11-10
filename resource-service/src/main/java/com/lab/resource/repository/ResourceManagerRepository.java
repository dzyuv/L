package com.lab.resource.repository;
import com.lab.resource.entity.ResourceManager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface ResourceManagerRepository extends CrudMapper<ResourceManager>{
    default List<ResourceManager> findByResourceIdOrderByIdAsc(Long resourceId) {
        return selectList(Wrappers.<ResourceManager>query().eq("resource_id", resourceId).orderByAsc("id"));
    }

    default boolean existsByUserIdAndManagerTypeIn(Long userId, List<String> managerTypes) {
        return selectCount(Wrappers.<ResourceManager>query()
                .eq("user_id", userId)
                .in("manager_type", managerTypes)) > 0;
    }
}
