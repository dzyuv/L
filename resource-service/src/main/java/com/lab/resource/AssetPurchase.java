package com.lab.resource;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("asset_purchase")
public class AssetPurchase {
    @TableId(type = IdType.AUTO) public Long id;
    public Long purchaserId;
    public String purchaserName;
    public LocalDateTime purchasedAt = LocalDateTime.now();
    public String source = "PURCHASE";
    public Long categoryId;
    public String name;
    public String brand;
    public String model;
    public int quantity;
    public Long resourceId;
    public String location;
    public String assetNos;
}
