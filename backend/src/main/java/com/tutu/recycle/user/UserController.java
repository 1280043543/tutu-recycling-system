package com.tutu.recycle.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.security.CurrentUser;
import com.tutu.recycle.user.entity.SysUser;
import com.tutu.recycle.user.mapper.SysUserMapper;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ApiResponse<Profile> me() {
        return ApiResponse.success(toProfile(findCurrentUser()));
    }

    @PutMapping
    public ApiResponse<Profile> update(@Valid @RequestBody UpdateProfileRequest request) {
        SysUser user = findCurrentUser();
        user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone());
        userMapper.updateById(user);
        return ApiResponse.success(toProfile(user));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        SysUser user = findCurrentUser();
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST.value(), "原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        return ApiResponse.success("密码修改成功", null);
    }

    private SysUser findCurrentUser() {
        SysUser user = userMapper.selectById(CurrentUser.id());
        if (user == null) throw new BusinessException(404, "用户不存在");
        return user;
    }

    private Profile toProfile(SysUser user) {
        return new Profile(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole(), user.getStatus());
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过50个字符")
        private String realName;
        @Size(max = 20, message = "联系电话不能超过20个字符")
        private String phone;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 72, message = "新密码长度应为8至72个字符")
        private String newPassword;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class Profile {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private String role;
        private Integer status;
    }
}
