package com.tutu.recycle.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId;
    private Long categoryId;
    private String categoryName;
    private Integer estimatedQuantity;
    private String itemDescription;
    private LocalDateTime createdAt;
}
