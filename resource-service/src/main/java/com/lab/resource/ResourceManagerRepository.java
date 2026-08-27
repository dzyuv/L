package com.lab.resource;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResourceManagerRepository extends JpaRepository<ResourceManager,Long>{
    Optional<ResourceManager> findFirstByResourceIdAndManagerTypeOrderByIdAsc(Long resourceId,String managerType);
}
