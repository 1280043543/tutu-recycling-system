package com.tutu.recycle.order.dto;

import com.tutu.recycle.order.entity.OrderItem;
import com.tutu.recycle.order.entity.OrderLog;
import com.tutu.recycle.order.entity.RecycleOrder;
import com.tutu.recycle.order.entity.RecycleRecord;
import com.tutu.recycle.order.entity.RecycleRecordItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderDetail {
    private RecycleOrder order;
    private List<OrderItem> items;
    private RecycleRecord recycleRecord;
    private List<RecycleRecordItem> resultItems;
    private List<OrderLog> logs;
}
