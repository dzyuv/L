package com.lab.user;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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
    default boolean existsByEmployeeNo(String no) {
        return selectCount(Wrappers.<User>query().eq("employee_no", no)) > 0;
    }
    default boolean existsByEmailIgnoreCase(String email) {
        return selectCount(Wrappers.<User>query().apply("LOWER(email)=LOWER({0})", email)) > 0;
    }
    default List<User> findByDeletedFalseOrderByCreatedAtDesc() {
        return selectList(Wrappers.<User>query().eq("deleted", false).orderByDesc("created_at"))
                .stream().map(this::hydrate).toList();
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

    private User hydrate(User user) {
        if (user == null) return null;
        for (Role role : selectRoles(user.id)) {
            role.permissions.addAll(selectPermissions(role.id));
            user.roles.add(role);
        }
        return user;
    }
}
