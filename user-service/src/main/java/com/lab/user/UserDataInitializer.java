package com.lab.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserDataInitializer {
    @Bean
    CommandLineRunner seedRoles(RoleRepository roles, PermissionRepository permissions, UserRepository users, PasswordEncoder encoder) {
        return args -> seed(roles, permissions, users, encoder);
    }

    @Transactional
    void seed(RoleRepository roles, PermissionRepository permissions, UserRepository users, PasswordEncoder encoder) {
        Role student=roles.findByCode("STUDENT").orElseGet(() -> {
            Role role=new Role(); role.code="STUDENT"; role.name="学生"; return roles.save(role);
        });
        Role teacher=roles.findByCode("TEACHER").orElseGet(() -> {
            Role role=new Role(); role.code="TEACHER"; role.name="Teacher"; return roles.save(role);
        });
        Role labAdmin=roles.findByCode("LAB_ADMIN").orElseGet(() -> {
            Role role=new Role(); role.code="LAB_ADMIN"; role.name="Laboratory administrator"; return roles.save(role);
        });
        Role systemAdmin=roles.findByCode("SYSTEM_ADMIN").orElseGet(() -> {
            Role role=new Role(); role.code="SYSTEM_ADMIN"; role.name="System administrator"; return roles.save(role);
        });
        List.of(
            new String[]{"resource:read","查看资源"},
            new String[]{"booking:create","创建预约"},
            new String[]{"booking:read:self","查看个人预约"},
            new String[]{"booking:cancel:self","取消个人预约"},
            new String[]{"booking:checkin","预约签到"}
        ).forEach(item -> {
            Permission permission=permissions.findAll().stream().filter(p -> item[0].equals(p.code)).findFirst().orElseGet(() -> { Permission p=new Permission(); p.code=item[0]; p.name=item[1]; return permissions.save(p); });
            student.permissions.add(permission);
        });
        roles.save(student);
        // Existing accounts keep their explicitly assigned roles. Only newly registered
        // accounts receive STUDENT in UserServiceImpl.register().
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
        createAdminIfMissing(users, encoder, "LAB20260001", "实验室管理员", "lab-admin@example.com", labAdmin, teacher);
        createAdminIfMissing(users, encoder, "ADMIN20260001", "系统管理员", "system-admin@example.com", systemAdmin);
    }

    private void createAdminIfMissing(UserRepository users, PasswordEncoder encoder, String username,
                                      String realName, String email, Role... assignedRoles) {
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
