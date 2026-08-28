package com.lab.resource;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("asset")
public class Asset {
    @TableId(type=IdType.AUTO) public Long id;
    public String assetNo;
    public String name;
    public Long categoryId;
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
