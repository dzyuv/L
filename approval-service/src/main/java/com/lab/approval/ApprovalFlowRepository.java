package com.lab.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
import java.util.Optional;

public interface ApprovalFlowRepository extends CrudMapper<ApprovalFlow> {
    default List<ApprovalFlow> findAllByOrderByResourceTypeIdAscVersionDesc() {
        return selectList(Wrappers.<ApprovalFlow>query().orderByAsc("resource_type_id").orderByDesc("version"));
    }
    default List<ApprovalFlow> findByResourceTypeIdOrderByVersionDesc(Long resourceTypeId) {
        return selectList(Wrappers.<ApprovalFlow>query().eq("resource_type_id", resourceTypeId).orderByDesc("version"));
    }

    default Optional<ApprovalFlow> findEnabledByResourceTypeId(Long resourceTypeId) {
        if (resourceTypeId == null) return Optional.empty();
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalFlow>query()
                .eq("resource_type_id", resourceTypeId).eq("enabled", true)
                .orderByDesc("version").last("LIMIT 1")));
    }
}
