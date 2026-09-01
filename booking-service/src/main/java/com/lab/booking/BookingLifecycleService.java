package com.lab.booking;

import com.lab.common.api.RuntimeSettings;
import com.lab.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class BookingLifecycleService {
    private final int noShowWindowDays;
    private final RuntimeSettings settings;

    private final BookingStatusHistoryRepository histories;
    private final BookingSlotRepository slots;
    private final ViolationRecordRepository violations;
    private final UserRestrictionRepository restrictions;

    BookingLifecycleService(BookingStatusHistoryRepository h,BookingSlotRepository s,ViolationRecordRepository v,UserRestrictionRepository r,
                            RuntimeSettings settings,
                            @Value("${booking.violation.window-days:30}") int noShowWindowDays){
        histories=h;
        slots=s;
        violations=v;
        restrictions=r;
        this.settings=settings;
        this.noShowWindowDays=noShowWindowDays;
    }

    public void assertCanCreate(Long userId){
        restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(userId,"ACTIVE",LocalDateTime.now())
            .ifPresent(restriction->{
                throw new BusinessException("USER_RESTRICTED","User is restricted from creating bookings until "+restriction.restrictedUntil,HttpStatus.FORBIDDEN);
            });
    }

    public void recordInitial(Booking booking,Long operatorId,String requestId){
        record(booking,null,booking.status,operatorId,null,requestId);
    }

    public void recordProgress(Booking booking,Long operatorId,String reason,String requestId){
        record(booking,booking.status,booking.status,operatorId,reason,requestId);
    }

    public void transition(Booking booking,String nextStatus,Long operatorId,String reason,String requestId){
        String previous=booking.status;
        booking.status=nextStatus;
        record(booking,previous,nextStatus,operatorId,reason,requestId);
    }

    public void releaseSlots(Long bookingId,String reason){
        LocalDateTime releasedAt=LocalDateTime.now();
        for(BookingSlot slot:slots.findByBookingIdAndReleasedAtIsNull(bookingId)){
            slot.releasedAt=releasedAt;
            slot.releaseReason=reason;
            slots.save(slot);
        }
    }

    public void recordNoShow(Booking booking){
        ViolationRecord violation=new ViolationRecord();
        violation.bookingId=booking.id;
        violation.userId=booking.userId;
        violation.violationType="NO_SHOW";
        violation.comment="Missed required check-in";
        violations.save(violation);

        LocalDateTime now=LocalDateTime.now();
        long count=violations.countActiveNoShows(booking.userId,now.minusDays(noShowWindowDays));
        boolean alreadyRestricted=restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(booking.userId,"ACTIVE",now).isPresent();
        if(count>=settings.violationMaxCount() && !alreadyRestricted){
            UserRestriction restriction=new UserRestriction();
            restriction.userId=booking.userId;
            restriction.restrictedUntil=now.plusDays(settings.restrictionDays());
            restriction.reason="Repeated no-show bookings";
            restriction.sourceViolationCount=(int)count;
            restrictions.save(restriction);
        }
    }

    public void refreshRestriction(Long userId){
        LocalDateTime now=LocalDateTime.now();
        long count=violations.countActiveNoShows(userId,now.minusDays(noShowWindowDays));
        if(count>=settings.violationMaxCount()) return;
        for(UserRestriction restriction:restrictions.findByUserIdAndStatus(userId,"ACTIVE")){
            if(restriction.restrictedUntil!=null && restriction.restrictedUntil.isAfter(now)){
                restriction.status="LIFTED";
                restrictions.save(restriction);
            }
        }
    }

    private void record(Booking booking,String from,String to,Long operatorId,String reason,String requestId){
        BookingStatusHistory history=new BookingStatusHistory();
        history.bookingId=booking.id;
        history.fromStatus=from;
        history.toStatus=to;
        history.operatorId=operatorId;
        history.reason=reason;
        history.requestId=requestId;
        histories.save(history);
    }
}
