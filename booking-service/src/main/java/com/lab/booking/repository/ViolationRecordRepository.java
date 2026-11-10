package com.lab.booking.repository;
import com.lab.booking.entity.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    default long countByStatus(String status) {
        return selectCount(Wrappers.<ViolationRecord>query().eq("status", status));
    }

    @Select("SELECT v.user_id AS userId, COALESCE(MAX(b.applicant_name_snapshot), CONCAT('用户 ', v.user_id)) AS name, COUNT(*) AS count " +
            "FROM violation_record v LEFT JOIN booking b ON b.id = v.booking_id " +
            "GROUP BY v.user_id ORDER BY count DESC, v.user_id ASC LIMIT #{limit}")
    List<Map<String, Object>> countGroupedByUser(@Param("limit") int limit);
}
