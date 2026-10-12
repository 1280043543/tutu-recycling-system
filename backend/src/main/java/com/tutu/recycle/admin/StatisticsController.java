package com.tutu.recycle.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.order.entity.RecycleOrder;
import com.tutu.recycle.order.entity.RecycleRecordItem;
import com.tutu.recycle.order.mapper.RecycleOrderMapper;
import com.tutu.recycle.order.mapper.RecycleRecordItemMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
public class StatisticsController {
    private final RecycleOrderMapper orderMapper;
    private final RecycleRecordItemMapper resultMapper;
    public StatisticsController(RecycleOrderMapper orderMapper, RecycleRecordItemMapper resultMapper) {
        this.orderMapper = orderMapper; this.resultMapper = resultMapper;
    }

    @GetMapping("/overview")
    public ApiResponse<Overview> overview(@RequestParam(required = false) LocalDateTime startTime,
                                          @RequestParam(required = false) LocalDateTime endTime) {
        QueryWrapper<RecycleOrder> query = orderWindow(startTime, endTime);
        List<RecycleOrder> orders = orderMapper.selectList(query);
        Map<String, Long> counts = new HashMap<String, Long>();
        for (RecycleOrder order : orders) counts.put(order.getStatus(), counts.containsKey(order.getStatus()) ? counts.get(order.getStatus()) + 1 : 1L);
        return ApiResponse.success(new Overview((long) orders.size(), counts));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryTotal>> categories(@RequestParam(required = false) LocalDateTime startTime,
                                                        @RequestParam(required = false) LocalDateTime endTime) {
        Map<Long, CategoryTotal> totals = new HashMap<Long, CategoryTotal>();
        for (RecycleRecordItem row : resultRows(startTime, endTime)) {
            CategoryTotal total = totals.get(row.getCategoryId());
            if (total == null) { total = new CategoryTotal(row.getCategoryId(), row.getCategoryName(), 0L); totals.put(row.getCategoryId(), total); }
            total.setActualQuantity(total.getActualQuantity() + row.getActualQuantity());
        }
        return ApiResponse.success(new ArrayList<CategoryTotal>(totals.values()));
    }

    @GetMapping("/weight")
    public ApiResponse<WeightTotal> weight(@RequestParam(required = false) LocalDateTime startTime,
                                            @RequestParam(required = false) LocalDateTime endTime) {
        BigDecimal total = BigDecimal.ZERO;
        for (RecycleRecordItem row : resultRows(startTime, endTime)) total = total.add(row.getActualWeight());
        return ApiResponse.success(new WeightTotal(total, "kg"));
    }

    private List<RecycleRecordItem> resultRows(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<RecycleRecordItem> query = new QueryWrapper<RecycleRecordItem>();
        if (startTime != null) query.ge("created_at", startTime);
        if (endTime != null) query.le("created_at", endTime);
        return resultMapper.selectList(query);
    }

    private QueryWrapper<RecycleOrder> orderWindow(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<RecycleOrder> query = new QueryWrapper<RecycleOrder>();
        if (startTime != null) query.ge("created_at", startTime);
        if (endTime != null) query.le("created_at", endTime);
        return query;
    }

    @Data @AllArgsConstructor public static class Overview {
        private long totalOrders; private Map<String, Long> statusCounts;
    }
    @Data @AllArgsConstructor public static class CategoryTotal {
        private Long categoryId; private String categoryName; private long actualQuantity;
    }
    @Data @AllArgsConstructor public static class WeightTotal {
        private BigDecimal totalWeight; private String weightUnit;
    }
}
