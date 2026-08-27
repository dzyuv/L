package com.lab.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalNodeRepository extends JpaRepository<ApprovalNode, Long> {
    List<ApprovalNode> findByFlowIdOrderByLevelAscSequenceNoAsc(Long flowId);
}
