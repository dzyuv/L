package com.lab.resource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ClosureRepository extends JpaRepository<ResourceClosure,Long>{
    List<ResourceClosure> findByResourceIdAndStatusNot(Long resourceId,String status);
}
