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
