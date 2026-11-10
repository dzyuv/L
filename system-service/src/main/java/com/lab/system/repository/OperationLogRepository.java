package com.lab.system.repository;
import com.lab.system.entity.OperationLog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationLogRepository extends CrudMapper<OperationLog> {
    default IPage<OperationLog> search(String operationType, String result, String keyword, Page<OperationLog> page) {
        QueryWrapper<OperationLog> wrapper = new QueryWrapper<>();
        if (operationType != null && !operationType.isBlank()) wrapper.eq("operation_type", operationType);
        if (result != null && !result.isBlank()) wrapper.eq("result", result);
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword + "%";
            wrapper.and(condition -> condition.like("operation_type", keyword)
                    .or().like("target_type", keyword)
                    .or().like("target_id", keyword)
                    .or().like("operator_id", keyword)
                    .or().like("request_id", keyword)
                    .or().like("reason", keyword)
                    .or().apply("CAST(detail AS CHAR) LIKE {0}", like));
        }
        wrapper.orderByDesc("created_at");
        return selectPage(page, wrapper);
    }

    default long deleteByCreatedAtBefore(LocalDateTime cutoff) {
        return delete(Wrappers.<OperationLog>query().lt("created_at", cutoff));
    }

    @Select("SELECT DISTINCT operation_type FROM operation_log WHERE operation_type IS NOT NULL AND operation_type <> '' ORDER BY operation_type")
    List<String> findDistinctOperationTypes();
}
