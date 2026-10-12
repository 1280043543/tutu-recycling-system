<script setup>
const props = defineProps({
  orders: { type: Array, default: () => [] },
  role: { type: String, default: '' },
})

defineEmits(['detail', 'cancel', 'confirm', 'object', 'review', 'approve', 'reject', 'assign', 'handle-exception', 'handle-cancellation', 'worker-action', 'result'])

function label(status) {
  return ({
    PENDING_REVIEW: '待审核', PENDING_ASSIGNMENT: '待分派', PENDING_ACCEPTANCE: '待回收人员接单',
    ACCEPTED: '已接单', PENDING_REASSIGNMENT: '待重新分派', IN_PROGRESS: '回收中',
    PENDING_RESIDENT_CONFIRMATION: '待居民确认', EXCEPTION_PENDING: '异常处理中',
    COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已驳回',
  })[status] || status || '未知'
}

function tone(status) {
  return ({
    PENDING_REVIEW: 'amber', PENDING_ASSIGNMENT: 'amber', PENDING_ACCEPTANCE: 'blue', ACCEPTED: 'blue',
    PENDING_REASSIGNMENT: 'amber', IN_PROGRESS: 'blue', PENDING_RESIDENT_CONFIRMATION: 'blue',
    EXCEPTION_PENDING: 'red', COMPLETED: 'green', CANCELLED: 'muted', REJECTED: 'muted',
  })[status] || 'muted'
}

function time(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '—' }
function appliance(order) { return order.items?.map((item) => `${item.categoryName} × ${item.estimatedQuantity}`).join(' · ') || order.orderNo }
</script>

<template>
  <div v-if="props.orders.length" class="order-list">
    <div v-for="order in props.orders" :key="order.id" class="order-row">
      <div class="order-glyph">♻</div>
      <div class="order-main">
        <strong>{{ appliance(order) }}</strong>
        <small>{{ order.orderNo }} <span>·</span> {{ time(order.appointmentTime) }}<span v-if="order.region"> · {{ order.region }} {{ order.detailAddress }}</span></small>
      </div>
      <span :class="['status-badge', tone(order.status)]"><i></i>{{ label(order.status) }}</span>
      <div class="row-actions">
        <button class="row-action detail-action" @click="$emit('detail', order)">查看详情</button>
        <template v-if="props.role === 'RESIDENT'">
          <button v-if="['PENDING_REVIEW', 'PENDING_ASSIGNMENT'].includes(order.status)" class="row-action" @click="$emit('cancel', order)">取消</button>
          <template v-if="order.status === 'PENDING_RESIDENT_CONFIRMATION'">
            <button class="row-action confirm-action" @click="$emit('confirm', order)">确认结果</button>
            <button class="row-action" @click="$emit('object', order)">有异议</button>
          </template>
          <button v-if="order.status === 'COMPLETED'" class="row-action confirm-action" @click="$emit('review', order)">评价</button>
        </template>
        <template v-if="props.role === 'ADMIN'">
          <button v-if="order.status === 'PENDING_REVIEW'" class="row-action confirm-action" @click="$emit('approve', order)">审核通过</button>
          <button v-if="order.status === 'PENDING_REVIEW'" class="row-action danger-text" @click="$emit('reject', order)">驳回</button>
          <button v-if="['PENDING_ASSIGNMENT', 'PENDING_REASSIGNMENT'].includes(order.status)" class="row-action confirm-action" @click="$emit('assign', order)">分派</button>
          <button v-if="order.status === 'EXCEPTION_PENDING'" class="row-action confirm-action" @click="$emit('handle-exception', order)">处理异常</button>
          <button v-if="['PENDING_ACCEPTANCE', 'ACCEPTED', 'IN_PROGRESS', 'PENDING_RESIDENT_CONFIRMATION', 'EXCEPTION_PENDING'].includes(order.status)" class="row-action" @click="$emit('handle-cancellation', order)">处理取消</button>
        </template>
        <template v-if="props.role === 'WORKER'">
          <button v-if="order.status === 'PENDING_ACCEPTANCE'" class="row-action confirm-action" @click="$emit('worker-action', order, 'accept')">接单</button>
          <button v-if="order.status === 'PENDING_ACCEPTANCE'" class="row-action danger-text" @click="$emit('worker-action', order, 'reject')">拒绝</button>
          <button v-if="order.status === 'ACCEPTED'" class="row-action confirm-action" @click="$emit('worker-action', order, 'start')">开始回收</button>
          <button v-if="order.status === 'IN_PROGRESS'" class="row-action confirm-action" @click="$emit('result', order)">登记结果</button>
          <button v-if="['IN_PROGRESS', 'ACCEPTED', 'PENDING_ACCEPTANCE'].includes(order.status)" class="row-action" @click="$emit('worker-action', order, 'exception')">异常</button>
        </template>
      </div>
    </div>
  </div>
  <div v-else class="empty-state">暂无订单记录</div>
</template>
