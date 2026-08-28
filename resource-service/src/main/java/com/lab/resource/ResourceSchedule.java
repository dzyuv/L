package com.lab.resource;
import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
@TableName("resource_schedule") public class ResourceSchedule{
    @TableId(type=IdType.AUTO) public Long id;
    public Long resourceId;
    public int weekday;
    public LocalTime openTime;
    public LocalTime closeTime;
    public boolean enabled=true;
    public int maxDurationMinutes=120;
    public int slotMinutes=30;
    public LocalDate effectiveFrom;
    public LocalDate effectiveTo;
    @Version public int version;
}
