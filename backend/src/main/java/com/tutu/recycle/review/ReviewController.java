package com.tutu.recycle.review;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.order.entity.RecycleOrder;
import com.tutu.recycle.order.mapper.RecycleOrderMapper;
import com.tutu.recycle.security.CurrentUser;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import java.util.List;

@Validated
@RestController
public class ReviewController {
    private final ServiceReviewMapper reviewMapper;
    private final RecycleOrderMapper orderMapper;
    public ReviewController(ServiceReviewMapper reviewMapper, RecycleOrderMapper orderMapper) {
        this.reviewMapper = reviewMapper; this.orderMapper = orderMapper;
    }

    @PostMapping("/api/orders/{id}/review")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<ServiceReview> create(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        RecycleOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "回收订单不存在");
        if (!order.getResidentId().equals(CurrentUser.id())) throw new BusinessException(403, "无权评价该订单");
        if (!"COMPLETED".equals(order.getStatus())) throw new BusinessException(409, "订单完成后才能评价");
        if (reviewMapper.selectCount(new QueryWrapper<ServiceReview>().eq("order_id", id)) > 0)
            throw new BusinessException(409, "该订单已经评价过");
        ServiceReview review = new ServiceReview(); review.setOrderId(id); review.setResidentId(CurrentUser.id());
        review.setRating(request.getRating()); review.setContent(request.getContent()); reviewMapper.insert(review);
        return ApiResponse.success("评价已提交", review);
    }

    @GetMapping("/api/admin/reviews")
    public ApiResponse<List<ServiceReview>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) throw new BusinessException(400, "分页参数不正确");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ServiceReview> page = reviewMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<ServiceReview>(pageNum, pageSize),
                new QueryWrapper<ServiceReview>().orderByDesc("created_at"));
        return ApiResponse.success(page.getRecords());
    }

    @Data
    public static class ReviewRequest {
        @NotNull(message = "请填写评分") @Min(value = 1, message = "评分最低为1分") @Max(value = 5, message = "评分最高为5分")
        private Integer rating;
        @Size(max = 1000, message = "评价内容不能超过1000个字符") private String content;
    }
}
