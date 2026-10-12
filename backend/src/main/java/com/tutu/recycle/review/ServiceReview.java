package com.tutu.recycle.review;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("service_review")
public class ServiceReview {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId;
    private Long residentId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
