package com.tutu.recycle.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.user.entity.SysUser;
import com.tutu.recycle.user.mapper.SysUserMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    public AdminUserController(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper; this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public ApiResponse<PageData<Profile>> list(@RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer status) {
        checkPage(pageNum, pageSize);
        QueryWrapper<SysUser> query = new QueryWrapper<SysUser>().select("id", "username", "real_name", "phone", "role", "status", "created_at");
        if (role != null && !role.trim().isEmpty()) query.eq("role", role);
        if (status != null) query.eq("status", status);
        if (keyword != null && !keyword.trim().isEmpty()) query.and(w -> w.like("username", keyword.trim())
                .or().like("real_name", keyword.trim()).or().like("phone", keyword.trim()));
        Page<SysUser> page = userMapper.selectPage(new Page<SysUser>(pageNum, pageSize), query.orderByDesc("created_at"));
        List<Profile> profiles = new java.util.ArrayList<Profile>();
        for (SysUser user : page.getRecords()) profiles.add(toProfile(user));
        return ApiResponse.success(new PageData<Profile>(profiles, page.getTotal(), pageNum, pageSize));
    }

    @PostMapping("/workers")
    public ApiResponse<Profile> createWorker(@Valid @RequestBody CreateWorkerRequest request) {
        if (userMapper.selectCount(new QueryWrapper<SysUser>().eq("username", request.getUsername().trim())) > 0)
            throw new BusinessException(409, "登录账号已存在");
        SysUser user = new SysUser(); user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword())); user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone()); user.setRole("WORKER"); user.setStatus(1);
        userMapper.insert(user);
        return ApiResponse.success("回收人员账号已创建", toProfile(user));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<Profile> get(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        return ApiResponse.success(toProfile(user));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<Void> status(@PathVariable Long id, @RequestBody StatusRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1))
            throw new BusinessException(400, "账号状态只能为0或1");
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if ("ADMIN".equals(user.getRole())) throw new BusinessException(400, "不能通过此接口停用管理员账号");
        user.setStatus(request.getStatus()); userMapper.updateById(user);
        return ApiResponse.success("账号状态已更新", null);
    }

    private Profile toProfile(SysUser user) {
        return new Profile(user.getId(), user.getUsername(), user.getRealName(), user.getPhone(), user.getRole(), user.getStatus());
    }
    private void checkPage(long page, long size) {
        if (page < 1 || size < 1 || size > 100) throw new BusinessException(400, "分页参数不正确");
    }

    @Data public static class CreateWorkerRequest {
        @NotBlank(message = "登录账号不能为空") @Size(max = 50) private String username;
        @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72) private String password;
        @NotBlank(message = "姓名不能为空") @Size(max = 50) private String realName;
        @Size(max = 20) private String phone;
    }
    @Data public static class StatusRequest { private Integer status; }
    @Data @AllArgsConstructor public static class Profile {
        private Long id; private String username; private String realName; private String phone; private String role; private Integer status;
    }
    @Data @AllArgsConstructor public static class PageData<T> {
        private List<T> records; private long total; private long pageNum; private long pageSize;
    }
}
