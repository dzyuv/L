package com.lab.statistics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface StatisticsSnapshotRepository extends CrudMapper<StatisticsSnapshot> {
    default List<StatisticsSnapshot> findTop100ByOrderByPeriodEndDesc() {
        return selectList(Wrappers.<StatisticsSnapshot>query().orderByDesc("period_end").last("LIMIT 100"));
    }
}
