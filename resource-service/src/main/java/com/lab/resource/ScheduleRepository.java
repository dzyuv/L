package com.lab.resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScheduleRepository extends JpaRepository<ResourceSchedule,Long>{
    List<ResourceSchedule> findByResourceIdAndWeekdayAndEnabledTrue(Long resourceId,int weekday);
    List<ResourceSchedule> findByResourceIdOrderByWeekdayAscOpenTimeAsc(Long resourceId);
}
