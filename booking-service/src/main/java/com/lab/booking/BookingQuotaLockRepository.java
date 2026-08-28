package com.lab.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingQuotaLockRepository extends JpaRepository<BookingQuotaLock, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO booking_quota_lock(user_id) VALUES (:userId)", nativeQuery = true)
    void ensureExists(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from BookingQuotaLock item where item.userId = :userId")
    Optional<BookingQuotaLock> lockByUserId(@Param("userId") Long userId);
}
