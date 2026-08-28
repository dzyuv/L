package com.lab.booking;

import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

public interface BookingQuotaLockRepository extends CrudMapper<BookingQuotaLock> {
    @Insert("INSERT IGNORE INTO booking_quota_lock(user_id) VALUES(#{userId})")
    void ensureExists(@Param("userId") Long userId);

    @Select("SELECT user_id FROM booking_quota_lock WHERE user_id=#{userId} FOR UPDATE")
    Optional<BookingQuotaLock> lockByUserId(@Param("userId") Long userId);
}
