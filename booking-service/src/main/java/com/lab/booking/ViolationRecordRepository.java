package com.lab.booking;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.time.LocalDateTime;
import java.util.List;

public interface ViolationRecordRepository extends CrudMapper<ViolationRecord>{
    default long countByUserIdAndViolationTypeAndCreatedAtAfter(Long userId,String violationType,LocalDateTime createdAt) {
        return selectCount(Wrappers.<ViolationRecord>query().eq("user_id", userId)
                .eq("violation_type", violationType).gt("created_at", createdAt));
    }
    default List<ViolationRecord> findAllByOrderByCreatedAtDesc() {
        return selectList(Wrappers.<ViolationRecord>query().orderByDesc("created_at"));
    }
}
