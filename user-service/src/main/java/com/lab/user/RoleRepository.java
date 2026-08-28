package com.lab.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lab.common.persistence.CrudMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends CrudMapper<Role> {
    default Optional<Role> findByCode(String code) {
        return Optional.ofNullable(hydrate(selectOne(Wrappers.<Role>query().eq("code", code))));
    }

    @Override
    default List<Role> findAll() {
        return CrudMapper.super.findAll().stream().map(this::hydrate).toList();
    }

    @Override
    default Role save(Role role) {
        CrudMapper.super.save(role);
        deletePermissions(role.id);
        role.permissions.stream().map(permission -> permission.id).filter(java.util.Objects::nonNull).distinct()
                .forEach(permissionId -> insertPermission(role.id, permissionId));
        return role;
    }

    @Select("SELECT p.* FROM permission p JOIN role_permission rp ON rp.permission_id=p.id WHERE rp.role_id=#{roleId} ORDER BY p.id")
    List<Permission> selectPermissions(@Param("roleId") Long roleId);

    @Delete("DELETE FROM role_permission WHERE role_id=#{roleId}")
    int deletePermissions(@Param("roleId") Long roleId);

    @Insert("INSERT INTO role_permission(role_id, permission_id) VALUES(#{roleId}, #{permissionId})")
    int insertPermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    private Role hydrate(Role role) {
        if (role != null) role.permissions.addAll(selectPermissions(role.id));
        return role;
    }
}
