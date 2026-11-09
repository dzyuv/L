package com.lab.resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface ResourceManagerRepository extends CrudMapper<ResourceManager>{
    default List<ResourceManager> findByResourceIdOrderByIdAsc(Long resourceId) {
        return selectList(Wrappers.<ResourceManager>query().eq("resource_id", resourceId).orderByAsc("id"));
    }
}
