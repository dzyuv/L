package com.lab.booking;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import java.time.LocalDateTime;

public interface ViolationRecordRepository extends CrudMapper<ViolationRecord>{
    default long countActiveNoShows(Long userId, LocalDateTime createdAt) {
        return selectCount(Wrappers.<ViolationRecord>query().eq("user_id", userId)
                .eq("violation_type", "NO_SHOW")
                .eq("status", "CONFIRMED")
                .gt("created_at", createdAt));
    }
    default IPage<ViolationRecord> search(Page<ViolationRecord> page) {
        return selectPage(page, Wrappers.<ViolationRecord>query().orderByDesc("created_at"));
    }
}
