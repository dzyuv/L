package com.lab.notification;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface NotificationRepository extends CrudMapper<Notification> {
    default List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId) {
        return selectList(Wrappers.<Notification>query().eq("user_id", userId).orderByDesc("created_at"));
    }
}
