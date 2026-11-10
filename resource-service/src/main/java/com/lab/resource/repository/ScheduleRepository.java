package com.lab.resource.repository;
import com.lab.resource.entity.*;
import com.lab.resource.client.*;
import com.lab.resource.seed.*;
import com.lab.resource.controller.*;
import com.lab.resource.service.*;
import com.lab.resource.service.impl.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;
public interface ScheduleRepository extends CrudMapper<ResourceSchedule>{
    default List<ResourceSchedule> findByResourceIdAndWeekdayAndEnabledTrue(Long resourceId,int weekday) {
        return selectList(Wrappers.<ResourceSchedule>query().eq("resource_id", resourceId)
                .eq("weekday", weekday).eq("enabled", true));
    }
    default List<ResourceSchedule> findByResourceIdOrderByWeekdayAscOpenTimeAsc(Long resourceId) {
        return selectList(Wrappers.<ResourceSchedule>query().eq("resource_id", resourceId)
                .orderByAsc("weekday", "open_time"));
    }
}
