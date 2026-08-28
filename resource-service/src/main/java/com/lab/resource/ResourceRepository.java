package com.lab.resource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
public interface ResourceRepository extends CrudMapper<Resource>{
    default long countByTypeIdAndDeletedFalse(Long typeId) {
        return selectCount(Wrappers.<Resource>query().eq("type_id", typeId).eq("deleted", false));
    }
}
