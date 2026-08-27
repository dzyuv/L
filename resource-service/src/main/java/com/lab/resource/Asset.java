package com.lab.resource;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset", uniqueConstraints = {
        @UniqueConstraint(columnNames = "assetNo"),
        @UniqueConstraint(columnNames = "serialNo")
})
public class Asset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String assetNo;
    @Column(nullable = false) public String name;
    @Column(nullable = false) public Long categoryId;
    public Long resourceId;
    public String serialNo;
    public String brand;
    public String model;
    public String specification;
    public String status = "IN_STOCK";
    public String location;
    public Long custodianUserId;
    public LocalDate purchaseDate;
    public LocalDate warrantyUntil;
    public BigDecimal originalCost;
    public String remark;
    public boolean deleted = false;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();
    @Version public int version;
}
