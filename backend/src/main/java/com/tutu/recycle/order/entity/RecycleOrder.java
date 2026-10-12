package com.tutu.recycle.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recycle_order")
public class RecycleOrder {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;
    private Long residentId;
    private Long workerId;
    private String status;
    private LocalDateTime appointmentTime;
    private String contactName;
    private String contactPhone;
    private String region;
    private String detailAddress;
    private String addressRemark;
    private String description;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
