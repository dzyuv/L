package com.lab.system;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@TableName(value="operation_log", autoResultMap=true)
public class OperationLog {
    @TableId(type=IdType.AUTO)
    public Long id;
    public Long operatorId;
    public String operationType;
    public String targetType;
    public Long targetId;
    public String result = "SUCCESS";
    public String reason;
    public String requestId;
    public String ip;
    @TableField(typeHandler=JacksonTypeHandler.class)
    public Map<String, Object> detail = new LinkedHashMap<>();
    public LocalDateTime createdAt = LocalDateTime.now();
}
