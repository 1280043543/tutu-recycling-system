package com.tutu.recycle.auth;

import com.tutu.recycle.user.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResult {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserProfile user;

    public static UserProfile from(SysUser user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole());
    }

    @Data
    @AllArgsConstructor
    public static class UserProfile {
        private Long id;
        private String username;
        private String realName;
        private String phone;
        private String role;
    }
}
