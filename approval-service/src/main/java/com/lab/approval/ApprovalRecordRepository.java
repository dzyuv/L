package com.lab.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.util.Optional;

public interface ApprovalRecordRepository extends CrudMapper<ApprovalRecord> {
    default Optional<ApprovalRecord> findByRequestId(String requestId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalRecord>query().eq("request_id", requestId)));
    }
    default Optional<ApprovalRecord> findByRequestIdAndTaskId(String requestId, Long taskId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalRecord>query().eq("request_id", requestId).eq("task_id", taskId)));
    }
}
