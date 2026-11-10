package com.lab.booking.approval.repository;
import com.lab.booking.entity.*;
import com.lab.booking.repository.*;
import com.lab.booking.client.*;
import com.lab.booking.scheduler.*;
import com.lab.booking.config.*;
import com.lab.booking.service.*;
import com.lab.booking.service.impl.*;
import com.lab.booking.controller.*;
import com.lab.booking.approval.entity.*;
import com.lab.booking.approval.service.*;
import com.lab.booking.approval.service.impl.*;
import com.lab.booking.approval.controller.*;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;

import java.util.Optional;

public interface ApprovalRecordRepository extends CrudMapper<ApprovalRecord> {
    default Optional<ApprovalRecord> findByRequestIdAndTaskId(String requestId, Long taskId) {
        return Optional.ofNullable(selectOne(Wrappers.<ApprovalRecord>query()
                .eq("request_id", requestId).eq("task_id", taskId)));
    }
}
