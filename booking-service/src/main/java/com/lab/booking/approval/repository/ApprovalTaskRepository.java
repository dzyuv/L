package com.lab.booking.approval.repository;
import com.lab.booking.approval.entity.ApprovalTask;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.api.Roles;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

public interface ApprovalTaskRepository extends CrudMapper<ApprovalTask> {
    default List<ApprovalTask> findByAssignedUserIdAndStatus(Long userId, String status) {
        return selectList(Wrappers.<ApprovalTask>query().eq("assigned_user_id", userId).eq("status", status));
    }

    default List<ApprovalTask> findPendingForLabAdmin(Long userId) {
        return selectList(Wrappers.<ApprovalTask>query()
                .eq("status", "PENDING")
                .and(wrapper -> wrapper.eq("assigned_user_id", userId)
                        .or(item -> item.eq("approver_role", Roles.LAB_ADMIN).isNull("assigned_user_id"))));
    }

    default List<ApprovalTask> findPendingForTeacher(Long userId) {
        return selectList(Wrappers.<ApprovalTask>query()
                .eq("status", "PENDING")
                .and(wrapper -> wrapper.eq("assigned_user_id", userId)
                        .or(item -> item.eq("approver_role", Roles.TEACHER).isNull("assigned_user_id"))));
    }

    default Optional<ApprovalTask> findByBookingId(Long bookingId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalTask>query()
                .eq("booking_id", bookingId).orderByDesc("level").last("LIMIT 1")));
    }

    default Optional<ApprovalTask> findByBookingIdAndLevel(Long bookingId, int level) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalTask>query()
                .eq("booking_id", bookingId).eq("level", level).last("LIMIT 1")));
    }

    default List<ApprovalTask> findByBookingIdAndStatus(Long bookingId, String status) {
        return selectList(Wrappers.<ApprovalTask>query().eq("booking_id", bookingId).eq("status", status));
    }

    default List<ApprovalTask> findByBookingIdAndStatusIn(Long bookingId, List<String> statuses) {
        return selectList(Wrappers.<ApprovalTask>query().eq("booking_id", bookingId).in("status", statuses));
    }

    @Update("UPDATE approval_task SET assigned_user_id = NULL WHERE assigned_user_id = #{userId} AND status = 'PENDING'")
    int releaseAssignedTo(@Param("userId") Long userId);
}
