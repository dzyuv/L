package com.lab.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface ApprovalFlowRepository extends CrudMapper<ApprovalFlow> {
    default List<ApprovalFlow> findAllByOrderByResourceTypeIdAscVersionDesc() {
        return selectList(Wrappers.<ApprovalFlow>query().orderByAsc("resource_type_id").orderByDesc("version"));
    }
    default List<ApprovalFlow> findByResourceTypeIdOrderByVersionDesc(Long resourceTypeId) {
        return selectList(Wrappers.<ApprovalFlow>query().eq("resource_type_id", resourceTypeId).orderByDesc("version"));
    }
}
