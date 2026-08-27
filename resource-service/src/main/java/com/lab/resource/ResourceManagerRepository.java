package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface ResourceManagerRepository extends JpaRepository<ResourceManager,Long>{
    Optional<ResourceManager> findFirstByResourceIdAndManagerTypeOrderByIdAsc(Long resourceId,String managerType);
    List<ResourceManager> findByResourceIdOrderByIdAsc(Long resourceId);
}
