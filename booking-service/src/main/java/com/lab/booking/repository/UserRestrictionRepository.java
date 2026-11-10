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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRestrictionRepository extends CrudMapper<UserRestriction>{
    default Optional<UserRestriction> findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(Long userId,String status,LocalDateTime time) {
        return Optional.ofNullable(selectOne(Wrappers.<UserRestriction>query().eq("user_id", userId)
                .eq("status", status).gt("restricted_until", time).orderByDesc("restricted_until").last("LIMIT 1")));
    }

    default List<UserRestriction> findByUserIdAndStatus(Long userId, String status) {
        return selectList(Wrappers.<UserRestriction>query().eq("user_id", userId).eq("status", status));
    }
}
