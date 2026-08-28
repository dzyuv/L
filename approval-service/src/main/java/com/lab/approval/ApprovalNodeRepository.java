package com.lab.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import java.util.List;

public interface ApprovalNodeRepository extends CrudMapper<ApprovalNode> {
    default List<ApprovalNode> findByFlowIdOrderByLevelAscSequenceNoAsc(Long flowId) {
        return selectList(Wrappers.<ApprovalNode>query().eq("flow_id", flowId).orderByAsc("level", "sequence_no"));
    }
}
