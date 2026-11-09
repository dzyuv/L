package com.lab.system;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogRepository extends CrudMapper<OperationLog> {
    default List<OperationLog> findAllByOrderByCreatedAtDesc() {
        return selectList(Wrappers.<OperationLog>query().orderByDesc("created_at"));
    }

    default long deleteByCreatedAtBefore(LocalDateTime cutoff) {
        return delete(Wrappers.<OperationLog>query().lt("created_at", cutoff));
    }
}
