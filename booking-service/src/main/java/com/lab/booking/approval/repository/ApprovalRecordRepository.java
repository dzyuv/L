package com.lab.booking.approval.repository;
import com.lab.booking.approval.entity.ApprovalRecord;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.util.Optional;

public interface ApprovalRecordRepository extends CrudMapper<ApprovalRecord> {
    default Optional<ApprovalRecord> findByRequestIdAndTaskId(String requestId, Long taskId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalRecord>query()
                .eq("request_id", requestId).eq("task_id", taskId)));
    }
}
