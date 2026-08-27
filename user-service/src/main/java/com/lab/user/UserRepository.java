package com.lab.user;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndDeletedFalse(String username);
    boolean existsByEmployeeNo(String no);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByDeletedFalseOrderByCreatedAtDesc();
}
