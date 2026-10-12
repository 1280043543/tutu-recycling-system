package com.tutu.recycle.auth;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.security.JwtTokenService;
import com.tutu.recycle.user.entity.SysUser;
import com.tutu.recycle.user.mapper.SysUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername().trim();
        Long count = userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", username));
        if (count != null && count > 0) {
            throw new BusinessException(HttpStatus.CONFLICT.value(), "该登录账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone());
        user.setRole("RESIDENT");
        user.setStatus(1);
        userMapper.insert(user);
    }

    public LoginResult login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new QueryWrapper<SysUser>().eq("username", request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED.value(), "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), "账号已停用，请联系管理员");
        }
        return new LoginResult(tokenService.createToken(user), "Bearer", tokenService.getExpirationSeconds(),
                LoginResult.from(user));
    }
}
