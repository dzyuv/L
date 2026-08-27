package com.lab.approval;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, Long> {
    List<ApprovalFlow> findAllByOrderByResourceTypeIdAscVersionDesc();
    List<ApprovalFlow> findByResourceTypeIdOrderByVersionDesc(Long resourceTypeId);
}
