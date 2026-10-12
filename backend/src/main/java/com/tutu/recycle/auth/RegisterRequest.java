package com.tutu.recycle.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度应为8至72个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名不能超过50个字符")
    private String realName;

    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String phone;
}
