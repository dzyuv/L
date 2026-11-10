package com.lab.resource.repository;
import com.lab.resource.entity.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
public interface ResourceRepository extends CrudMapper<Resource>{
    default long countByTypeIdAndDeletedFalse(Long typeId) {
        return selectCount(Wrappers.<Resource>query().eq("type_id", typeId).eq("deleted", false));
    }
}
