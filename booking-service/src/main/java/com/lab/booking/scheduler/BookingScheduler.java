package com.lab.booking.scheduler;

import com.lab.booking.service.BookingService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingScheduler {
    private final BookingService service;

    public BookingScheduler(BookingService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "booking_auto_complete", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void autoComplete() { service.autoComplete(); }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "booking_mark_no_show", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void markNoShow() { service.markNoShow(); }

    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "booking_expire_approvals", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void expirePendingApprovals() { service.expirePendingApprovals(); }
}
