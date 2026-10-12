package com.tutu.recycle.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.user.entity.SysUser;
import com.tutu.recycle.user.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {
    @Value("${app.bootstrap-admin.username:}") private String username;
    @Value("${app.bootstrap-admin.password:}") private String password;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper; this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 8) return;
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username.trim())) > 0) return;
        SysUser admin = new SysUser(); admin.setUsername(username.trim());
        admin.setPassword(passwordEncoder.encode(password)); admin.setRealName("系统管理员");
        admin.setRole("ADMIN"); admin.setStatus(1); userMapper.insert(admin);
    }
}
