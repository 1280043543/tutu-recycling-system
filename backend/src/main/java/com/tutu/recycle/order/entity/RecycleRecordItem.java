package com.tutu.recycle.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recycle_record_item")
public class RecycleRecordItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long recordId;
    private Long categoryId;
    private String categoryName;
    private Integer actualQuantity;
    private BigDecimal actualWeight;
    private String weightUnit;
    private LocalDateTime createdAt;
}
