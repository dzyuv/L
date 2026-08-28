package com.lab.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class BookingLifecycleService {
    private static final int NO_SHOW_THRESHOLD=3;
    private static final int NO_SHOW_WINDOW_DAYS=30;
    private static final int RESTRICTION_DAYS=30;

    private final BookingStatusHistoryRepository histories;
    private final BookingSlotRepository slots;
    private final ViolationRecordRepository violations;
    private final UserRestrictionRepository restrictions;
    private final OutboxEventRepository outbox;
    private final ObjectMapper json;

    BookingLifecycleService(BookingStatusHistoryRepository h,BookingSlotRepository s,ViolationRecordRepository v,UserRestrictionRepository r,OutboxEventRepository o,ObjectMapper objectMapper){
        histories=h;
        slots=s;
        violations=v;
        restrictions=r;
        outbox=o;
        json=objectMapper;
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
        long count=violations.countByUserIdAndViolationTypeAndCreatedAtAfter(booking.userId,"NO_SHOW",now.minusDays(NO_SHOW_WINDOW_DAYS));
        boolean alreadyRestricted=restrictions.findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(booking.userId,"ACTIVE",now).isPresent();
        if(count>=NO_SHOW_THRESHOLD && !alreadyRestricted){
            UserRestriction restriction=new UserRestriction();
            restriction.userId=booking.userId;
            restriction.restrictedUntil=now.plusDays(RESTRICTION_DAYS);
            restriction.reason="Repeated no-show bookings";
            restriction.sourceViolationCount=(int)count;
            restrictions.save(restriction);
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
        writeStatusEvent(booking,from,to,operatorId,reason,requestId);
    }

    private void writeStatusEvent(Booking booking,String from,String to,Long operatorId,String reason,String requestId){
        Map<String,Object> payload=new LinkedHashMap<>();
        payload.put("bookingId",booking.id);
        payload.put("bookingNo",booking.bookingNo);
        payload.put("userId",booking.userId);
        payload.put("resourceId",booking.resourceId);
        payload.put("fromStatus",from);
        payload.put("toStatus",to);
        payload.put("operatorId",operatorId);
        payload.put("reason",reason);
        payload.put("requestId",requestId);
        OutboxEvent event=new OutboxEvent();
        event.eventId=UUID.randomUUID().toString();
        event.eventType="booking.status.changed";
        event.aggregateType="BOOKING";
        event.aggregateId=booking.id;
        try{
            event.payload=json.writeValueAsString(payload);
        }catch(JsonProcessingException exception){
            throw new IllegalStateException("Cannot serialize booking outbox event",exception);
        }
        outbox.save(event);
    }
}
