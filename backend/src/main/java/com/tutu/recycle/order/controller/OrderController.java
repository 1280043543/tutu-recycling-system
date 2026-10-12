package com.tutu.recycle.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tutu.recycle.common.ApiResponse;
import com.tutu.recycle.order.dto.OrderDetail;
import com.tutu.recycle.order.dto.OrderRequests;
import com.tutu.recycle.order.entity.RecycleOrder;
import com.tutu.recycle.order.service.OrderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResponseEntity<ApiResponse<RecycleOrder>> create(@Valid @RequestBody OrderRequests.CreateOrder request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("订单提交成功", orderService.create(request)));
    }

    @GetMapping("/orders/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<PageData<RecycleOrder>> mine(@RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String status) {
        return ApiResponse.success(toPage(orderService.residentOrders(pageNum, pageSize, status)));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<OrderDetail> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id, false));
    }

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<Void> cancel(@PathVariable Long id, @RequestBody(required = false) OrderRequests.Reason request) {
        orderService.cancel(id, request == null ? null : request.getReason());
        return ApiResponse.success("订单已取消", null);
    }

    @PostMapping("/orders/{id}/confirm-result")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        orderService.confirmResult(id, false, null);
        return ApiResponse.success("回收结果已确认", null);
    }

    @PostMapping("/orders/{id}/raise-objection")
    @PreAuthorize("hasRole('RESIDENT')")
    public ApiResponse<Void> objection(@PathVariable Long id, @Valid @RequestBody OrderRequests.Reason request) {
        orderService.confirmResult(id, true, request.getReason());
        return ApiResponse.success("异议已提交管理员处理", null);
    }

    @GetMapping("/admin/orders")
    public ApiResponse<PageData<RecycleOrder>> adminList(@RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderNo, @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long workerId, @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime) {
        return ApiResponse.success(toPage(orderService.adminOrders(pageNum, pageSize, status, orderNo, residentId, workerId, startTime, endTime)));
    }

    @GetMapping("/admin/orders/{id}")
    public ApiResponse<OrderDetail> adminDetail(@PathVariable Long id) {
        return ApiResponse.success(orderService.detail(id, true));
    }

    @PostMapping("/admin/orders/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        orderService.approve(id); return ApiResponse.success("审核通过", null);
    }

    @PostMapping("/admin/orders/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody OrderRequests.Reason request) {
        orderService.reject(id, request.getReason()); return ApiResponse.success("订单已驳回", null);
    }

    @PostMapping("/admin/orders/{id}/assign")
    public ApiResponse<Void> assign(@PathVariable Long id, @Valid @RequestBody OrderRequests.Assignment request) {
        orderService.assign(id, request, false); return ApiResponse.success("分派成功", null);
    }

    @PostMapping("/admin/orders/{id}/reassign")
    public ApiResponse<Void> reassign(@PathVariable Long id, @Valid @RequestBody OrderRequests.Assignment request) {
        orderService.assign(id, request, true); return ApiResponse.success("重新分派成功", null);
    }

    @PostMapping("/admin/orders/{id}/handle-cancellation")
    public ApiResponse<Void> handleCancellation(@PathVariable Long id, @Valid @RequestBody OrderRequests.CancellationDecision request) {
        orderService.handleCancellation(id, request); return ApiResponse.success("取消请求已处理", null);
    }

    @PostMapping("/admin/orders/{id}/handle-exception")
    public ApiResponse<Void> handleException(@PathVariable Long id, @Valid @RequestBody OrderRequests.ExceptionResolution request) {
        orderService.handleException(id, request.getResolution()); return ApiResponse.success("异常已处理", null);
    }

    @GetMapping("/worker/tasks")
    public ApiResponse<PageData<RecycleOrder>> workerTasks(@RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String status) {
        return ApiResponse.success(toPage(orderService.workerOrders(pageNum, pageSize, status)));
    }

    @GetMapping("/worker/tasks/{orderId}")
    public ApiResponse<OrderDetail> workerDetail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.workerDetail(orderId));
    }

    @PostMapping("/worker/tasks/{orderId}/accept")
    public ApiResponse<Void> accept(@PathVariable Long orderId) {
        orderService.accept(orderId, true, null); return ApiResponse.success("已确认接单", null);
    }

    @PostMapping("/worker/tasks/{orderId}/reject")
    public ApiResponse<Void> rejectTask(@PathVariable Long orderId, @Valid @RequestBody OrderRequests.Reason request) {
        orderService.accept(orderId, false, request.getReason()); return ApiResponse.success("已拒绝任务", null);
    }

    @PostMapping("/worker/tasks/{orderId}/start")
    public ApiResponse<Void> start(@PathVariable Long orderId) {
        orderService.start(orderId); return ApiResponse.success("回收任务已开始", null);
    }

    @PostMapping("/worker/tasks/{orderId}/result")
    public ApiResponse<Void> result(@PathVariable Long orderId, @Valid @RequestBody OrderRequests.Result request) {
        orderService.recordResult(orderId, request); return ApiResponse.success("回收结果已登记", null);
    }

    @PostMapping("/worker/tasks/{orderId}/exception")
    public ApiResponse<Void> exception(@PathVariable Long orderId, @Valid @RequestBody OrderRequests.Reason request) {
        orderService.workerException(orderId, request.getReason()); return ApiResponse.success("异常已提交", null);
    }

    private PageData<RecycleOrder> toPage(Page<RecycleOrder> page) {
        return new PageData<RecycleOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Data @AllArgsConstructor
    public static class PageData<T> {
        private List<T> records; private long total; private long pageNum; private long pageSize;
    }
}
