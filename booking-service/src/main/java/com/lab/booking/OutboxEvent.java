package com.lab.booking;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="outbox_event")
public class OutboxEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true) public String eventId;
    public String eventType;
    public String aggregateType;
    public Long aggregateId;
    @Column(columnDefinition="json") public String payload;
    public String status="PENDING";
    public int retryCount;
    public String lastError;
    public LocalDateTime nextRetryAt;
    public LocalDateTime sentAt;
    public LocalDateTime createdAt=LocalDateTime.now();
}
