package com.lab.booking;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.time.LocalDateTime;
import java.util.List;

public interface ViolationRecordRepository extends CrudMapper<ViolationRecord>{
    default long countActiveNoShows(Long userId, LocalDateTime createdAt) {
        return selectCount(Wrappers.<ViolationRecord>query().eq("user_id", userId)
                .eq("violation_type", "NO_SHOW")
                .eq("status", "CONFIRMED")
                .gt("created_at", createdAt));
    }
    default List<ViolationRecord> findAllByOrderByCreatedAtDesc() {
        return selectList(Wrappers.<ViolationRecord>query().orderByDesc("created_at"));
    }
}
