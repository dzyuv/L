package com.lab.user.seed;
import com.lab.user.entity.Permission;
import com.lab.user.repository.PermissionRepository;
import com.lab.user.entity.Role;
import com.lab.user.repository.RoleRepository;
import com.lab.user.entity.User;
import com.lab.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserSeedService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserSeedService(RoleRepository roles, PermissionRepository permissions,
                           UserRepository users, PasswordEncoder encoder) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void seedRequiredRoles() {
        Role student = ensureRole("STUDENT", "学生");
        ensureRole("TEACHER", "Teacher");
        ensureRole("LAB_ADMIN", "Laboratory administrator");
        ensureRole("SYSTEM_ADMIN", "System administrator");
        List.of(
                new String[]{"resource:read", "查看资源"},
                new String[]{"booking:create", "创建预约"},
                new String[]{"booking:read:self", "查看个人预约"},
                new String[]{"booking:cancel:self", "取消个人预约"},
                new String[]{"booking:checkin", "预约签到"}
        ).forEach(item -> {
            Permission permission = permissions.findAll().stream()
                    .filter(existing -> item[0].equals(existing.code))
                    .findFirst()
                    .orElseGet(() -> {
                        Permission created = new Permission();
                        created.code = item[0];
                        created.name = item[1];
                        return permissions.save(created);
                    });
            student.permissions.add(permission);
        });
        roles.save(student);
    }

    @Transactional
    public void seedDemoUsers() {
        Role student = requiredRole("STUDENT");
        Role teacher = requiredRole("TEACHER");
        Role labAdmin = requiredRole("LAB_ADMIN");
        Role systemAdmin = requiredRole("SYSTEM_ADMIN");
        if (users.findByUsername("S20260001").isEmpty()) {
            User demo = new User();
            demo.employeeNo = "S20260001";
            demo.username = "S20260001";
            demo.realName = "张三";
            demo.passwordHash = encoder.encode("12345678");
            demo.email = "demo@example.com";
            demo.roles.add(student);
            users.save(demo);
        }
        if (users.findByUsername("T20260001").isEmpty()) {
            User demo = new User();
            demo.employeeNo = "T20260001";
            demo.username = "T20260001";
            demo.realName = "Teacher Demo";
            demo.passwordHash = encoder.encode("12345678");
            demo.email = "teacher@example.com";
            demo.roles.add(teacher);
            users.save(demo);
        }
        createDemoUserIfMissing("LAB20260001", "实验室管理员", "lab-admin@example.com", labAdmin, teacher);
        createDemoUserIfMissing("ADMIN20260001", "系统管理员", "system-admin@example.com", systemAdmin);
    }

    private Role ensureRole(String code, String name) {
        return roles.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.code = code;
            role.name = name;
            return roles.save(role);
        });
    }

    private Role requiredRole(String code) {
        return roles.findByCode(code).orElseThrow(() -> new IllegalStateException(code + " role is not initialized"));
    }

    private void createDemoUserIfMissing(String username, String realName, String email, Role... assignedRoles) {
        if (users.findByUsername(username).isPresent()) return;
        User user = new User();
        user.employeeNo = username;
        user.username = username;
        user.realName = realName;
        user.passwordHash = encoder.encode("12345678");
        user.email = email;
        user.roles.addAll(List.of(assignedRoles));
        users.save(user);
    }
}
