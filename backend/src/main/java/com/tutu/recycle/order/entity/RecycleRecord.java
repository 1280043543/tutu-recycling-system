package com.tutu.recycle.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("recycle_record")
public class RecycleRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId;
    private Long workerId;
    private String remark;
    private LocalDateTime recordedAt;
    private LocalDateTime residentConfirmedAt;
}
