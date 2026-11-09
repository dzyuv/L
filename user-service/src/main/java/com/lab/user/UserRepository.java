package com.lab.user;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
public interface UserRepository extends CrudMapper<User>{
    default Optional<User> findByUsername(String username) {
        return Optional.ofNullable(hydrate(selectOne(Wrappers.<User>query().eq("username", username))));
    }
    default Optional<User> findByUsernameAndDeletedFalse(String username) {
        return Optional.ofNullable(hydrate(selectOne(Wrappers.<User>query()
                .eq("username", username).eq("deleted", false))));
    }
    default Optional<User> findByUsernameAndDeletedFalseForUpdate(String username) {
        return Optional.ofNullable(hydrate(selectOne(Wrappers.<User>query()
                .eq("username", username).eq("deleted", false).last("FOR UPDATE"))));
    }
    default boolean existsByEmployeeNo(String no) {
        return selectCount(Wrappers.<User>query().eq("employee_no", no)) > 0;
    }
    default boolean existsByUsername(String username) {
        return selectCount(Wrappers.<User>query().eq("username", username)) > 0;
    }
    default List<User> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<User>query().eq("deleted", false).orderByDesc("created_at"))
                .stream().map(this::hydrate).toList();
    }

    default IPage<User> search(String status, String keyword, Page<User> page) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false);
        if (status != null && !status.isBlank()) wrapper.eq("status", status);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(condition -> condition.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("email", keyword));
        }
        wrapper.orderByDesc("created_at");
        IPage<User> result = selectPage(page, wrapper);
        result.setRecords(result.getRecords().stream().map(this::hydrate).toList());
        return result;
    }

    @Override
    default Optional<User> findById(Long id) {
        return Optional.ofNullable(hydrate(selectById(id)));
    }

    @Override
    default User save(User user) {
        CrudMapper.super.save(user);
        deleteRoles(user.id);
        user.roles.stream().map(role -> role.id).filter(java.util.Objects::nonNull).distinct()
                .forEach(roleId -> insertRole(user.id, roleId));
        return user;
    }

    @Select("SELECT r.* FROM role r JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=#{userId} ORDER BY r.id")
    List<Role> selectRoles(@Param("userId") Long userId);

    @Select("SELECT p.* FROM permission p JOIN role_permission rp ON rp.permission_id=p.id WHERE rp.role_id=#{roleId} ORDER BY p.id")
    List<Permission> selectPermissions(@Param("roleId") Long roleId);

    @Delete("DELETE FROM user_role WHERE user_id=#{userId}")
    int deleteRoles(@Param("userId") Long userId);

    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Update("UPDATE `user` SET status=#{status}, failed_login_count=#{failedLoginCount}, " +
            "locked_until=#{lockedUntil}, last_login_at=#{lastLoginAt} WHERE id=#{id}")
    int updateLoginState(@Param("id") Long id, @Param("status") String status,
                         @Param("lockedUntil") Instant lockedUntil,
                         @Param("failedLoginCount") int failedLoginCount,
                         @Param("lastLoginAt") Instant lastLoginAt);

    private User hydrate(User user) {
        if (user == null) return null;
        for (Role role : selectRoles(user.id)) {
            role.permissions.addAll(selectPermissions(role.id));
            user.roles.add(role);
        }
        return user;
    }
}
