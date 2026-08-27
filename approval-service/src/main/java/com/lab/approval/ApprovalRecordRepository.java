package com.lab.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {
    Optional<ApprovalRecord> findByRequestId(String requestId);
    Optional<ApprovalRecord> findByRequestIdAndTaskId(String requestId, Long taskId);
}
