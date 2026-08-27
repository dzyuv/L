package com.lab.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ViolationRecordRepository extends JpaRepository<ViolationRecord,Long>{
    long countByUserIdAndViolationTypeAndCreatedAtAfter(Long userId,String violationType,LocalDateTime createdAt);
    List<ViolationRecord> findAllByOrderByCreatedAtDesc();
}
