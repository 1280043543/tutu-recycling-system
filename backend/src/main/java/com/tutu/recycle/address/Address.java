package com.tutu.recycle.address;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_address")
public class Address {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String contactName;
    private String contactPhone;
    private String region;
    private String detailAddress;
    private String addressRemark;
    private Integer isDefault;
    private Integer isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
