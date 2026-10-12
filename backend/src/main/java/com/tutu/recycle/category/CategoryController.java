package com.tutu.recycle.category;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.common.BusinessException;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
public class CategoryController {
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<Category>> enabledCategories() {
        return ApiResponse.success(categoryMapper.selectList(
                new QueryWrapper<Category>().eq("status", 1).orderByAsc("id")));
    }

    @GetMapping("/api/categories/{id}")
    public ApiResponse<Category> category(@PathVariable Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(404, "回收品类不存在");
        return ApiResponse.success(category);
    }

    @GetMapping("/api/admin/categories")
    public ApiResponse<PageData<Category>> adminCategories(@RequestParam(defaultValue = "1") long pageNum,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(400, "分页参数不正确");
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Category> page =
                categoryMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Category>(pageNum, pageSize),
                        new QueryWrapper<Category>().orderByAsc("id"));
        return ApiResponse.success(new PageData<Category>(page.getRecords(), page.getTotal(), pageNum, pageSize));
    }

    @PostMapping("/api/admin/categories")
    public ApiResponse<Category> create(@Valid @RequestBody CategoryRequest request) {
        Long duplicate = categoryMapper.selectCount(new QueryWrapper<Category>().eq("name", request.getName().trim()));
        if (duplicate != null && duplicate > 0) throw new BusinessException(409, "该品类名称已存在");
        Category category = new Category();
        copy(request, category);
        category.setStatus(1);
        categoryMapper.insert(category);
        return ApiResponse.success("创建成功", category);
    }

    @PutMapping("/api/admin/categories/{id}")
    public ApiResponse<Category> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(404, "回收品类不存在");
        Long duplicate = categoryMapper.selectCount(new QueryWrapper<Category>()
                .eq("name", request.getName().trim()).ne("id", id));
        if (duplicate != null && duplicate > 0) throw new BusinessException(409, "该品类名称已存在");
        copy(request, category);
        categoryMapper.updateById(category);
        return ApiResponse.success(category);
    }

    @PatchMapping("/api/admin/categories/{id}/status")
    public ApiResponse<Void> setStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException(400, "品类状态只能为0或1");
        }
        Category category = categoryMapper.selectById(id);
        if (category == null) throw new BusinessException(404, "回收品类不存在");
        category.setStatus(request.getStatus());
        categoryMapper.updateById(category);
        return ApiResponse.success("状态更新成功", null);
    }

    private void copy(CategoryRequest request, Category category) {
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setRecycleRequirements(request.getRecycleRequirements());
    }

    @Data
    public static class CategoryRequest {
        @NotBlank(message = "品类名称不能为空")
        @Size(max = 100, message = "品类名称不能超过100个字符")
        private String name;
        @Size(max = 500, message = "品类说明不能超过500个字符")
        private String description;
        @Size(max = 500, message = "回收要求不能超过500个字符")
        private String recycleRequirements;
    }

    @Data
    public static class StatusRequest { private Integer status; }

    @Data
    @lombok.AllArgsConstructor
    public static class PageData<T> {
        private List<T> records;
        private long total;
        private long pageNum;
        private long pageSize;
    }
}
