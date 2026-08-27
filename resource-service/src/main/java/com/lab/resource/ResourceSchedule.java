package com.lab.resource;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
@Entity public class ResourceSchedule{
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
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
