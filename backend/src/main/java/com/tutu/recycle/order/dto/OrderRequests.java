package com.tutu.recycle.order.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderRequests {
    private OrderRequests() { }

    @Data
    public static class CreateOrder {
        @NotNull(message = "请选择回收地址") private Long addressId;
        @NotNull(message = "请选择预约上门时间") private LocalDateTime appointmentTime;
        @Size(max = 1000, message = "补充说明不能超过1000个字符") private String description;
        @NotEmpty(message = "请至少选择一种回收品类")
        @Valid private List<CreateItem> items;
    }

    @Data
    public static class CreateItem {
        @NotNull(message = "请选择回收品类") private Long categoryId;
        @NotNull(message = "请填写预计数量") @Min(value = 1, message = "预计数量至少为1") private Integer estimatedQuantity;
        @Size(max = 500, message = "家电说明不能超过500个字符") private String itemDescription;
    }

    @Data
    public static class Reason {
        @NotBlank(message = "请填写原因") @Size(max = 500, message = "原因不能超过500个字符") private String reason;
    }

    @Data
    public static class Assignment {
        @NotNull(message = "请选择回收人员") private Long workerId;
        @Size(max = 500, message = "说明不能超过500个字符") private String comment;
    }

    @Data
    public static class Result {
        @Size(max = 500, message = "备注不能超过500个字符") private String remark;
        @NotEmpty(message = "请填写至少一项回收结果") @Valid private List<ResultItem> items;
    }

    @Data
    public static class ResultItem {
        @NotNull(message = "请选择品类") private Long categoryId;
        @NotNull(message = "请填写实际数量") @Min(value = 0, message = "实际数量不能小于0") private Integer actualQuantity;
        @NotNull(message = "请填写实际重量") @DecimalMin(value = "0.0", message = "实际重量不能小于0") private BigDecimal actualWeight;
        @Size(max = 10, message = "重量单位不能超过10个字符") private String weightUnit;
    }

    @Data
    public static class CancellationDecision {
        @NotNull(message = "请说明是否同意取消") private Boolean approved;
        @NotBlank(message = "请填写处理意见") @Size(max = 500, message = "处理意见不能超过500个字符") private String comment;
    }

    @Data
    public static class ExceptionResolution {
        @NotBlank(message = "请填写处理结果") @Size(max = 500, message = "处理结果不能超过500个字符") private String resolution;
    }
}
