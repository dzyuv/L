package com.lab.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface ViolationRecordRepository extends JpaRepository<ViolationRecord,Long>{
    long countByUserIdAndViolationTypeAndCreatedAtAfter(Long userId,String violationType,LocalDateTime createdAt);
}
