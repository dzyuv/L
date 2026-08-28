package com.lab.booking;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.*;
public interface BookingRepository extends CrudMapper<Booking>{
    default Optional<Booking> findByUserIdAndClientRequestId(Long userId,String key) {
        return Optional.ofNullable(selectOne(Wrappers.<Booking>query().eq("user_id", userId).eq("client_request_id", key)));
    }
    default List<Booking> findByUserIdOrderByStartTimeDesc(Long userId) {
        return selectList(Wrappers.<Booking>query().eq("user_id", userId).orderByDesc("start_time"));
    }
}
