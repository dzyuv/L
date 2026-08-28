package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.Optional;
import java.util.List;

public interface ResourceManagerRepository extends CrudMapper<ResourceManager>{
    default Optional<ResourceManager> findFirstByResourceIdAndManagerTypeOrderByIdAsc(Long resourceId,String managerType) {
        return Optional.ofNullable(selectOne(Wrappers.<ResourceManager>query().eq("resource_id", resourceId)
                .eq("manager_type", managerType).orderByAsc("id").last("LIMIT 1")));
    }
    default List<ResourceManager> findByResourceIdOrderByIdAsc(Long resourceId) {
        return selectList(Wrappers.<ResourceManager>query().eq("resource_id", resourceId).orderByAsc("id"));
    }
}
