package com.lab.resource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
public interface ClosureRepository extends CrudMapper<ResourceClosure>{
    default List<ResourceClosure> findByResourceIdAndStatusNot(Long resourceId,String status) {
        return selectList(Wrappers.<ResourceClosure>query().eq("resource_id", resourceId).ne("status", status));
    }
}
