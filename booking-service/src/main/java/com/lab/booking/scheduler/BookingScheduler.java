package com.lab.booking.scheduler;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.repository.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

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
