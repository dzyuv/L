package com.lab.resource;
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
