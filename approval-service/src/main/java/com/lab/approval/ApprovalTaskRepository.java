package com.lab.approval;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask,Long>{
    List<ApprovalTask> findByAssignedUserIdAndStatus(Long userId,String status);
    Optional<ApprovalTask> findByBookingId(Long bookingId);
}
