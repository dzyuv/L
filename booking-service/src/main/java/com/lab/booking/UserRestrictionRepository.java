package com.lab.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRestrictionRepository extends JpaRepository<UserRestriction,Long>{
    Optional<UserRestriction> findFirstByUserIdAndStatusAndRestrictedUntilAfterOrderByRestrictedUntilDesc(Long userId,String status,LocalDateTime time);
}
