package com.tutu.recycle.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tutu.recycle.address.Address;
import com.tutu.recycle.address.AddressMapper;
import com.tutu.recycle.category.Category;
import com.tutu.recycle.category.CategoryMapper;
import com.tutu.recycle.common.BusinessException;
import com.tutu.recycle.order.dto.OrderDetail;
import com.tutu.recycle.order.dto.OrderRequests;
import com.tutu.recycle.order.entity.OrderItem;
import com.tutu.recycle.order.entity.OrderLog;
import com.tutu.recycle.order.entity.RecycleOrder;
import com.tutu.recycle.order.entity.RecycleRecord;
import com.tutu.recycle.order.entity.RecycleRecordItem;
import com.tutu.recycle.order.mapper.OrderItemMapper;
import com.tutu.recycle.order.mapper.OrderLogMapper;
import com.tutu.recycle.order.mapper.RecycleOrderMapper;
import com.tutu.recycle.order.mapper.RecycleRecordItemMapper;
import com.tutu.recycle.order.mapper.RecycleRecordMapper;
import com.tutu.recycle.security.CurrentUser;
import com.tutu.recycle.user.entity.SysUser;
import com.tutu.recycle.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final RecycleOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final RecycleRecordMapper recordMapper;
    private final RecycleRecordItemMapper resultItemMapper;
    private final OrderLogMapper logMapper;
    private final AddressMapper addressMapper;
    private final CategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public OrderService(RecycleOrderMapper orderMapper, OrderItemMapper itemMapper,
                        RecycleRecordMapper recordMapper, RecycleRecordItemMapper resultItemMapper,
                        OrderLogMapper logMapper, AddressMapper addressMapper,
                        CategoryMapper categoryMapper, SysUserMapper userMapper) {
        this.orderMapper = orderMapper; this.itemMapper = itemMapper;
        this.recordMapper = recordMapper; this.resultItemMapper = resultItemMapper;
        this.logMapper = logMapper; this.addressMapper = addressMapper;
        this.categoryMapper = categoryMapper; this.userMapper = userMapper;
    }

    @Transactional
    public RecycleOrder create(OrderRequests.CreateOrder request) {
        Long residentId = CurrentUser.id();
        Address address = addressMapper.selectOne(new QueryWrapper<Address>().eq("id", request.getAddressId())
                .eq("user_id", residentId).eq("is_deleted", 0));
        if (address == null) throw new BusinessException(400, "请选择本人有效的回收地址");
        if (!request.getAppointmentTime().isAfter(LocalDateTime.now()))
            throw new BusinessException(400, "预约时间必须晚于当前时间");

        RecycleOrder order = new RecycleOrder();
        order.setOrderNo("TT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        order.setResidentId(residentId);
        order.setStatus("PENDING_REVIEW");
        order.setAppointmentTime(request.getAppointmentTime());
        order.setContactName(address.getContactName()); order.setContactPhone(address.getContactPhone());
        order.setRegion(address.getRegion()); order.setDetailAddress(address.getDetailAddress());
        order.setAddressRemark(address.getAddressRemark()); order.setDescription(request.getDescription());
        orderMapper.insert(order);

        for (OrderRequests.CreateItem requestedItem : request.getItems()) {
            Category category = categoryMapper.selectById(requestedItem.getCategoryId());
            if (category == null || category.getStatus() == null || category.getStatus() != 1)
                throw new BusinessException(400, "申请中包含未启用或不存在的回收品类");
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId()); item.setCategoryId(category.getId());
            item.setCategoryName(category.getName()); item.setEstimatedQuantity(requestedItem.getEstimatedQuantity());
            item.setItemDescription(requestedItem.getItemDescription());
            itemMapper.insert(item);
        }
        log(order, residentId, "SUBMITTED", null, "PENDING_REVIEW", "居民提交回收申请");
        return order;
    }

    public Page<RecycleOrder> residentOrders(long pageNum, long pageSize, String status) {
        checkPage(pageNum, pageSize);
        QueryWrapper<RecycleOrder> query = new QueryWrapper<RecycleOrder>().eq("resident_id", CurrentUser.id());
        if (status != null && !status.trim().isEmpty()) query.eq("status", status);
        return orderMapper.selectPage(new Page<RecycleOrder>(pageNum, pageSize), query.orderByDesc("created_at"));
    }

    public Page<RecycleOrder> workerOrders(long pageNum, long pageSize, String status) {
        checkPage(pageNum, pageSize);
        QueryWrapper<RecycleOrder> query = new QueryWrapper<RecycleOrder>().eq("worker_id", CurrentUser.id());
        if (status != null && !status.trim().isEmpty()) query.eq("status", status);
        return orderMapper.selectPage(new Page<RecycleOrder>(pageNum, pageSize), query.orderByDesc("created_at"));
    }

    public Page<RecycleOrder> adminOrders(long pageNum, long pageSize, String status, String orderNo,
                                           Long residentId, Long workerId, LocalDateTime startTime, LocalDateTime endTime) {
        checkPage(pageNum, pageSize);
        QueryWrapper<RecycleOrder> query = new QueryWrapper<RecycleOrder>();
        if (status != null && !status.trim().isEmpty()) query.eq("status", status);
        if (orderNo != null && !orderNo.trim().isEmpty()) query.like("order_no", orderNo.trim());
        if (residentId != null) query.eq("resident_id", residentId);
        if (workerId != null) query.eq("worker_id", workerId);
        if (startTime != null) query.ge("created_at", startTime);
        if (endTime != null) query.le("created_at", endTime);
        return orderMapper.selectPage(new Page<RecycleOrder>(pageNum, pageSize), query.orderByDesc("created_at"));
    }

    public OrderDetail detail(Long orderId, boolean admin) {
        RecycleOrder order = requireOrder(orderId);
        if (!admin && !order.getResidentId().equals(CurrentUser.id())) throw new BusinessException(403, "无权查看该订单");
        return buildDetail(order);
    }

    public OrderDetail workerDetail(Long orderId) {
        RecycleOrder order = requireOrder(orderId);
        if (order.getWorkerId() == null || !order.getWorkerId().equals(CurrentUser.id()))
            throw new BusinessException(403, "该任务未分派给当前回收人员");
        return buildDetail(order);
    }

    @Transactional
    public void cancel(Long id, String reason) {
        RecycleOrder order = requireOrder(id);
        if (!order.getResidentId().equals(CurrentUser.id())) throw new BusinessException(403, "无权取消该订单");
        if (!isStatus(order, "PENDING_REVIEW", "PENDING_ASSIGNMENT"))
            throw new BusinessException(409, "订单已分派或已进入后续流程，请联系管理员处理");
        String old = order.getStatus(); order.setStatus("CANCELLED"); order.setCancelReason(reason);
        orderMapper.updateById(order);
        log(order, CurrentUser.id(), "RESIDENT_CANCELLED", old, "CANCELLED", reason);
    }

    @Transactional
    public void approve(Long id) {
        RecycleOrder order = requireOrder(id); requireStatus(order, "PENDING_REVIEW");
        change(order, "PENDING_ASSIGNMENT", "APPROVED", "管理员审核通过");
    }

    @Transactional
    public void reject(Long id, String reason) {
        RecycleOrder order = requireOrder(id); requireStatus(order, "PENDING_REVIEW");
        change(order, "REJECTED", "REJECTED", reason);
    }

    @Transactional
    public void assign(Long id, OrderRequests.Assignment request, boolean reassign) {
        RecycleOrder order = requireOrder(id);
        if (reassign) requireStatus(order, "PENDING_REASSIGNMENT", "PENDING_ASSIGNMENT", "PENDING_ACCEPTANCE");
        else requireStatus(order, "PENDING_ASSIGNMENT");
        SysUser worker = userMapper.selectById(request.getWorkerId());
        if (worker == null || !"WORKER".equals(worker.getRole()) || worker.getStatus() == null || worker.getStatus() != 1)
            throw new BusinessException(400, "请选择有效的启用中回收人员");
        String old = order.getStatus(); order.setWorkerId(worker.getId()); order.setStatus("PENDING_ACCEPTANCE");
        orderMapper.updateById(order);
        log(order, CurrentUser.id(), reassign ? "REASSIGNED" : "ASSIGNED", old, order.getStatus(), request.getComment());
    }

    @Transactional
    public void handleCancellation(Long id, OrderRequests.CancellationDecision request) {
        RecycleOrder order = requireOrder(id);
        requireStatus(order, "PENDING_ACCEPTANCE", "ACCEPTED", "IN_PROGRESS", "PENDING_RESIDENT_CONFIRMATION", "EXCEPTION_PENDING");
        String old = order.getStatus();
        if (Boolean.TRUE.equals(request.getApproved())) {
            order.setStatus("CANCELLED"); order.setCancelReason(request.getComment()); orderMapper.updateById(order);
        }
        log(order, CurrentUser.id(), "CANCELLATION_HANDLED", old, order.getStatus(), request.getComment());
    }

    @Transactional
    public void handleException(Long id, String resolution) {
        RecycleOrder order = requireOrder(id); requireStatus(order, "EXCEPTION_PENDING");
        String next = recordMapper.selectOne(new QueryWrapper<RecycleRecord>().eq("order_id", id)) == null
                ? "PENDING_REASSIGNMENT" : "PENDING_RESIDENT_CONFIRMATION";
        change(order, next, "EXCEPTION_HANDLED", resolution);
    }

    @Transactional
    public void accept(Long id, boolean accept, String reason) {
        RecycleOrder order = requireOrder(id); requireWorker(order); requireStatus(order, "PENDING_ACCEPTANCE");
        String next = accept ? "ACCEPTED" : "PENDING_REASSIGNMENT";
        change(order, next, accept ? "WORKER_ACCEPTED" : "WORKER_REJECTED", accept ? "回收人员确认接单" : reason);
    }

    @Transactional
    public void start(Long id) {
        RecycleOrder order = requireOrder(id); requireWorker(order); requireStatus(order, "ACCEPTED");
        change(order, "IN_PROGRESS", "RECYCLE_STARTED", "回收人员开始执行任务");
    }

    @Transactional
    public void workerException(Long id, String reason) {
        RecycleOrder order = requireOrder(id); requireWorker(order);
        requireStatus(order, "PENDING_ACCEPTANCE", "ACCEPTED", "IN_PROGRESS");
        change(order, "EXCEPTION_PENDING", "WORKER_EXCEPTION", reason);
    }

    @Transactional
    public void recordResult(Long id, OrderRequests.Result request) {
        RecycleOrder order = requireOrder(id); requireWorker(order); requireStatus(order, "IN_PROGRESS");
        List<OrderItem> orderItems = itemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", id));
        List<Long> allowed = new ArrayList<Long>();
        for (OrderItem item : orderItems) allowed.add(item.getCategoryId());
        RecycleRecord record = new RecycleRecord(); record.setOrderId(id); record.setWorkerId(CurrentUser.id());
        record.setRemark(request.getRemark()); recordMapper.insert(record);
        for (OrderRequests.ResultItem result : request.getItems()) {
            if (!allowed.contains(result.getCategoryId())) throw new BusinessException(400, "回收结果包含订单未申报的品类");
            if (result.getWeightUnit() != null && !result.getWeightUnit().trim().isEmpty()
                    && !"kg".equalsIgnoreCase(result.getWeightUnit().trim()))
                throw new BusinessException(400, "本系统回收重量统一使用kg");
            Category category = categoryMapper.selectById(result.getCategoryId());
            RecycleRecordItem row = new RecycleRecordItem();
            row.setRecordId(record.getId()); row.setCategoryId(category.getId()); row.setCategoryName(category.getName());
            row.setActualQuantity(result.getActualQuantity()); row.setActualWeight(result.getActualWeight());
            row.setWeightUnit("kg");
            resultItemMapper.insert(row);
        }
        change(order, "PENDING_RESIDENT_CONFIRMATION", "RESULT_RECORDED", "回收人员登记实际数量和重量");
    }

    @Transactional
    public void confirmResult(Long id, boolean objection, String reason) {
        RecycleOrder order = requireOrder(id);
        if (!order.getResidentId().equals(CurrentUser.id())) throw new BusinessException(403, "无权确认该订单");
        requireStatus(order, "PENDING_RESIDENT_CONFIRMATION");
        RecycleRecord record = recordMapper.selectOne(new QueryWrapper<RecycleRecord>().eq("order_id", id));
        if (record == null) throw new BusinessException(409, "订单尚无回收结果");
        if (objection) {
            change(order, "EXCEPTION_PENDING", "RESIDENT_OBJECTED", reason);
        } else {
            record.setResidentConfirmedAt(LocalDateTime.now()); recordMapper.updateById(record);
            order.setCompletedAt(LocalDateTime.now());
            change(order, "COMPLETED", "RESIDENT_CONFIRMED", "居民确认回收结果");
        }
    }

    private OrderDetail buildDetail(RecycleOrder order) {
        OrderDetail detail = new OrderDetail(); detail.setOrder(order);
        detail.setItems(itemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", order.getId())));
        RecycleRecord record = recordMapper.selectOne(new QueryWrapper<RecycleRecord>().eq("order_id", order.getId()));
        detail.setRecycleRecord(record);
        detail.setResultItems(record == null ? new ArrayList<RecycleRecordItem>() :
                resultItemMapper.selectList(new QueryWrapper<RecycleRecordItem>().eq("record_id", record.getId())));
        detail.setLogs(logMapper.selectList(new QueryWrapper<OrderLog>().eq("order_id", order.getId()).orderByAsc("created_at")));
        return detail;
    }

    private RecycleOrder requireOrder(Long id) {
        RecycleOrder order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException(404, "回收订单不存在");
        return order;
    }

    private void requireWorker(RecycleOrder order) {
        if (order.getWorkerId() == null || !order.getWorkerId().equals(CurrentUser.id()))
            throw new BusinessException(403, "该任务未分派给当前回收人员");
    }

    private boolean isStatus(RecycleOrder order, String... allowed) {
        for (String status : allowed) if (status.equals(order.getStatus())) return true;
        return false;
    }

    private void requireStatus(RecycleOrder order, String... allowed) {
        if (!isStatus(order, allowed)) throw new BusinessException(409, "当前订单状态不允许该操作");
    }

    private void change(RecycleOrder order, String next, String action, String comment) {
        String old = order.getStatus(); order.setStatus(next);
        if ("COMPLETED".equals(next)) order.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log(order, CurrentUser.id(), action, old, next, comment);
    }

    private void log(RecycleOrder order, Long operator, String action, String from, String to, String comment) {
        OrderLog log = new OrderLog(); log.setOrderId(order.getId()); log.setOperatorId(operator);
        log.setAction(action); log.setFromStatus(from); log.setToStatus(to); log.setComment(comment); logMapper.insert(log);
    }

    private void checkPage(long pageNum, long pageSize) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) throw new BusinessException(400, "分页参数不正确");
    }
}
