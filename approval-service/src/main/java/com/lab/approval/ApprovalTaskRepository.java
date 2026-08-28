package com.lab.approval;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.*;
public interface ApprovalTaskRepository extends CrudMapper<ApprovalTask>{
    default List<ApprovalTask> findByAssignedUserIdAndStatus(Long userId,String status) {
        return selectList(Wrappers.<ApprovalTask>query().eq("assigned_user_id", userId).eq("status", status));
    }
    default Optional<ApprovalTask> findByBookingId(Long bookingId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalTask>query().eq("booking_id", bookingId).last("LIMIT 1")));
    }
}
