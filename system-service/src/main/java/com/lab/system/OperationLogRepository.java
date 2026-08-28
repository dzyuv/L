package com.lab.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.util.List;

public interface OperationLogRepository extends CrudMapper<OperationLog> {
    default List<OperationLog> findAllByOrderByCreatedAtDesc() {
        return selectList(Wrappers.<OperationLog>query().orderByDesc("created_at"));
    }
}
