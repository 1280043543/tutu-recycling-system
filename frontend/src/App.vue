<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from './api/http'
import OrderTable from './components/OrderTable.vue'

const token = ref(localStorage.getItem('accessToken') || '')
const user = ref(JSON.parse(localStorage.getItem('currentUser') || 'null'))
const active = ref('工作台')
const busy = ref(false)
const notice = ref('')
const error = ref('')
const authMode = ref('login')
const authForm = reactive({ username: '', password: '', realName: '', phone: '' })
const profile = reactive({ realName: '', phone: '', oldPassword: '', newPassword: '' })
const data = reactive({ orders: [], addresses: [], categories: [], workers: [], users: [], stats: null, reviews: [] })
const addressForm = reactive({ contactName: '', contactPhone: '', region: '', detailAddress: '', addressRemark: '', isDefault: true })
const addressEditId = ref(null)
const orderForm = reactive({ addressId: '', appointmentTime: '', description: '', items: [] })
const categoryForm = reactive({ id: null, name: '', description: '', recycleRequirements: '' })
const workerForm = reactive({ username: '', password: '', realName: '', phone: '' })
const filters = reactive({ status: '', keyword: '' })
const page = ref(1)
const total = ref(0)
const selectedOrder = ref(null)
const detailOrder = ref(null)
const detailLoading = ref(false)
const reviewOrder = ref(null)
const reviewSubmitting = ref(false)
const reviewForm = reactive({ rating: 5, content: '' })
const resultForm = reactive({ remark: '', items: [] })
const cancelReason = ref('')
const rejectReason = ref('')
const workerComment = ref('')
const roleName = computed(() => ({ RESIDENT: '居民端', WORKER: '回收人员端', ADMIN: '管理员端' }[user.value?.role] || '未登录'))
const nav = computed(() => ({
  RESIDENT: ['工作台', '提交回收申请', '我的订单', '回收地址', '回收品类', '个人资料'],
  WORKER: ['工作台', '我的任务', '个人资料'],
  ADMIN: ['工作台', '订单审核', '订单管理', '用户管理', '回收品类', '服务评价', '业务统计', '个人资料'],
}[user.value?.role] || []))
const dashboardActions = computed(() => ({
  RESIDENT: [
    { title: '提交回收申请', hint: '预约家电上门回收', icon: '＋', action: '提交回收申请', tone: 'orange' },
    { title: '跟踪我的订单', hint: '查看进度与回收结果', icon: '▤', action: '我的订单', tone: 'gold' },
    { title: '管理回收地址', hint: '维护联系人和上门地址', icon: '⌖', action: '回收地址', tone: 'peach' },
    { title: '浏览回收品类', hint: '查看可回收家电范围', icon: '♻', action: '回收品类', tone: 'green' },
  ],
  WORKER: [
    { title: '查看我的任务', hint: '接单并更新回收进度', icon: '▤', action: '我的任务', tone: 'orange' },
    { title: '同步最新任务', hint: '从服务端刷新任务状态', icon: '↻', action: 'refresh', tone: 'gold' },
    { title: '维护个人资料', hint: '更新姓名与联系电话', icon: '♙', action: '个人资料', tone: 'peach' },
  ],
  ADMIN: [
    { title: '审核新订单', hint: '检查居民提交的回收申请', icon: '✓', action: '订单审核', tone: 'orange' },
    { title: '管理订单', hint: '分派任务并跟进异常', icon: '▤', action: '订单管理', tone: 'gold' },
    { title: '管理用户', hint: '维护居民与回收人员账号', icon: '♙', action: '用户管理', tone: 'peach' },
    { title: '查看业务统计', hint: '了解订单和回收重量', icon: '▥', action: '业务统计', tone: 'green' },
  ],
}[user.value?.role] || []))
const pageTitle = computed(() => active.value)
const displayOrders = computed(() => {
  let list = data.orders
  if (filters.status) list = list.filter((o) => o.status === filters.status)
  if (filters.keyword.trim()) {
    const q = filters.keyword.trim().toLowerCase()
    list = list.filter((o) => `${o.orderNo} ${o.contactName} ${o.detailAddress}`.toLowerCase().includes(q))
  }
  return list
})
const statusText = (status) => ({
  PENDING_REVIEW: '待审核', PENDING_ASSIGNMENT: '待分派', PENDING_ACCEPTANCE: '待回收人员接单',
  ACCEPTED: '已接单', PENDING_REASSIGNMENT: '待重新分派', IN_PROGRESS: '回收中', PENDING_RESIDENT_CONFIRMATION: '待居民确认',
  EXCEPTION_PENDING: '异常处理中', COMPLETED: '已完成', CANCELLED: '已取消', REJECTED: '已驳回',
}[status] || status || '未知')
const orderProgress = [
  { title: '提交申请', description: '居民提交回收预约' },
  { title: '审核与分派', description: '管理员审核订单并安排回收人员' },
  { title: '人员接单', description: '回收人员确认服务任务' },
  { title: '上门回收', description: '回收人员上门并登记实际结果' },
  { title: '居民确认', description: '居民核对实际数量与重量' },
  { title: '订单完成', description: '回收服务流程结束' },
]
const progressIndex = (status) => ({
  PENDING_REVIEW: 0, PENDING_ASSIGNMENT: 1, PENDING_REASSIGNMENT: 1,
  PENDING_ACCEPTANCE: 2, ACCEPTED: 2, IN_PROGRESS: 3, EXCEPTION_PENDING: 3,
  PENDING_RESIDENT_CONFIRMATION: 4, COMPLETED: 5,
}[status] ?? -1)
const logActionText = (action) => ({
  SUBMITTED: '居民提交申请', APPROVED: '管理员审核通过', REJECTED: '管理员驳回订单',
  ASSIGNED: '管理员分派任务', REASSIGNED: '管理员重新分派任务', WORKER_ACCEPTED: '回收人员已接单',
  WORKER_REJECTED: '回收人员拒绝接单', RECYCLE_STARTED: '回收人员开始执行任务', RESULT_RECORDED: '回收人员登记结果',
  RESIDENT_CONFIRMED: '居民确认回收结果', RESIDENT_OBJECTED: '居民提交结果异议',
  RESIDENT_CANCELLED: '居民取消订单', CANCELLATION_HANDLED: '管理员处理取消申请', WORKER_EXCEPTION: '回收人员上报异常',
  EXCEPTION_HANDLED: '管理员处理异常',
}[action] || action || '订单状态更新')
const statusClass = (status) => ({
  PENDING_REVIEW: 'amber', PENDING_ASSIGNMENT: 'amber', PENDING_ACCEPTANCE: 'blue',
  ACCEPTED: 'blue', PENDING_REASSIGNMENT: 'amber', IN_PROGRESS: 'blue', PENDING_RESIDENT_CONFIRMATION: 'blue',
  EXCEPTION_PENDING: 'red', COMPLETED: 'green', CANCELLED: 'muted', REJECTED: 'muted',
}[status] || 'muted')
const prettyTime = (v) => v ? String(v).replace('T', ' ').slice(0, 16) : '—'
const apiError = (e) => e?.message || '请求失败，请检查后端服务和网络'

function toast(message, isError = false) {
  notice.value = message
  error.value = isError ? message : ''
  window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 3200)
}

function responseData(response) { return response?.data }
function requireLogin() {
  token.value = ''; user.value = null
  localStorage.removeItem('accessToken'); localStorage.removeItem('currentUser')
  toast('登录状态已失效，请重新登录', true)
}

async function login() {
  busy.value = true
  try {
    const result = responseData(await http.post('/auth/login', { username: authForm.username, password: authForm.password }))
    token.value = result.accessToken
    user.value = result.user
    localStorage.setItem('accessToken', result.accessToken)
    localStorage.setItem('currentUser', JSON.stringify(result.user))
    active.value = '工作台'
    await loadPage()
    toast('登录成功，欢迎回来')
  } catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}

async function register() {
  busy.value = true
  try {
    await http.post('/auth/register', { username: authForm.username, password: authForm.password, realName: authForm.realName, phone: authForm.phone || null })
    authMode.value = 'login'
    toast('注册成功，请登录')
  } catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}

async function logout() {
  try { if (token.value) await http.post('/auth/logout') } catch { /* 本地令牌仍需清除 */ }
  token.value = ''; user.value = null; data.orders = []
  localStorage.removeItem('accessToken'); localStorage.removeItem('currentUser')
  active.value = '工作台'; authForm.password = ''; toast('已退出登录')
}

async function loadMe() {
  const me = responseData(await http.get('/users/me'))
  user.value = me
  localStorage.setItem('currentUser', JSON.stringify(me))
  profile.realName = me.realName || ''; profile.phone = me.phone || ''
}

async function loadOrders() {
  let result
  if (user.value.role === 'RESIDENT') result = responseData(await http.get('/orders/mine', { params: { pageNum: page.value, pageSize: 50, status: filters.status || undefined } }))
  if (user.value.role === 'WORKER') result = responseData(await http.get('/worker/tasks', { params: { pageNum: page.value, pageSize: 50, status: filters.status || undefined } }))
  if (user.value.role === 'ADMIN') result = responseData(await http.get('/admin/orders', { params: { pageNum: page.value, pageSize: 50, status: filters.status || undefined, orderNo: filters.keyword || undefined } }))
  data.orders = result?.records || []
  total.value = result?.total || 0
}

async function loadAddresses() { data.addresses = responseData(await http.get('/addresses')) || [] }
async function loadCategories(admin = false) {
  const result = responseData(await http.get(admin ? '/admin/categories' : '/categories', admin ? { params: { pageNum: 1, pageSize: 100 } } : undefined))
  data.categories = admin ? (result?.records || []) : (result || [])
  if (!orderForm.items.length) orderForm.items = data.categories.slice(0, 1).map((c) => ({ categoryId: c.id, estimatedQuantity: 1, itemDescription: '' }))
}
async function loadWorkers() {
  const result = responseData(await http.get('/admin/users', { params: { pageNum: 1, pageSize: 100, role: 'WORKER', status: 1 } }))
  data.workers = result?.records || []
}
async function loadUsers() {
  const result = responseData(await http.get('/admin/users', { params: { pageNum: 1, pageSize: 100 } }))
  data.users = result?.records || []
}
async function loadReviews() {
  data.reviews = responseData(await http.get('/admin/reviews', { params: { pageNum: 1, pageSize: 100 } })) || []
}
async function loadStats() {
  const [overview, categories, weight] = await Promise.all([
    http.get('/admin/statistics/overview'), http.get('/admin/statistics/categories'), http.get('/admin/statistics/weight'),
  ])
  data.stats = { overview: responseData(overview), categories: responseData(categories), weight: responseData(weight) }
}

async function loadPage() {
  if (!user.value) return
  busy.value = true; error.value = ''
  try {
    if (['工作台', '我的订单', '订单审核', '订单管理', '我的任务'].includes(active.value)) await loadOrders()
    if (['工作台', '提交回收申请', '回收地址'].includes(active.value) && user.value.role === 'RESIDENT') await loadAddresses()
    if (['工作台', '提交回收申请', '回收品类'].includes(active.value)) await loadCategories(user.value.role === 'ADMIN')
    if (['工作台', '订单分派'].includes(active.value) && user.value.role === 'ADMIN') await loadWorkers()
    if (active.value === '用户管理' && user.value.role === 'ADMIN') await loadUsers()
    if (active.value === '服务评价' && user.value.role === 'ADMIN') await loadReviews()
    if (active.value === '业务统计' && user.value.role === 'ADMIN') await loadStats()
    if (active.value === '个人资料') await loadMe()
    if (active.value === '订单分派' || (active.value === '订单管理' && user.value.role === 'ADMIN')) await loadWorkers()
  } catch (e) {
    if (e?.status === 401) requireLogin()
    toast(apiError(e), true)
  } finally { busy.value = false }
}

async function go(item) { active.value = item; page.value = 1; await loadPage() }
async function runDashboardAction(action) {
  if (action === 'refresh') await refresh()
  else await go(action)
}
async function saveAddress() {
  busy.value = true
  try {
    if (addressEditId.value) await http.put(`/addresses/${addressEditId.value}`, addressForm)
    else await http.post('/addresses', addressForm)
    addressEditId.value = null
    Object.assign(addressForm, { contactName: '', contactPhone: '', region: '', detailAddress: '', addressRemark: '', isDefault: true })
    await loadAddresses(); toast('回收地址已保存')
  }
  catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}
function editAddress(a) { addressEditId.value = a.id; Object.assign(addressForm, { contactName: a.contactName, contactPhone: a.contactPhone, region: a.region, detailAddress: a.detailAddress, addressRemark: a.addressRemark || '', isDefault: a.isDefault === 1 }) }
async function setDefaultAddress(a) { try { await http.put(`/addresses/${a.id}/default`); await loadAddresses(); toast('默认地址已更新') } catch (e) { toast(apiError(e), true) } }
async function deleteAddress(a) { if (!window.confirm(`确认删除地址“${a.region} ${a.detailAddress}”吗？`)) return; try { await http.delete(`/addresses/${a.id}`); await loadAddresses(); toast('地址已删除') } catch (e) { toast(apiError(e), true) } }
function addOrderItem() { orderForm.items.push({ categoryId: data.categories[0]?.id || '', estimatedQuantity: 1, itemDescription: '' }) }
function removeOrderItem(index) { orderForm.items.splice(index, 1) }
async function submitOrder() {
  busy.value = true
  try {
    const payload = { ...orderForm, addressId: Number(orderForm.addressId), items: orderForm.items.map((i) => ({ ...i, categoryId: Number(i.categoryId), estimatedQuantity: Number(i.estimatedQuantity) })) }
    await http.post('/orders', payload)
    toast('回收申请已提交')
    Object.assign(orderForm, { addressId: '', appointmentTime: '', description: '', items: data.categories.slice(0, 1).map((c) => ({ categoryId: c.id, estimatedQuantity: 1, itemDescription: '' })) })
    await go('我的订单')
  } catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}
async function cancelOrder(order) {
  const reason = window.prompt('请输入取消原因（可留空）')
  if (reason === null) return
  try { await http.post(`/orders/${order.id}/cancel`, { reason }); await loadOrders(); toast('订单已取消') } catch (e) { toast(apiError(e), true) }
}
async function confirmResult(order) {
  if (!window.confirm('确认回收人员登记的数量和重量无误，并完成此订单？')) return
  try { await http.post(`/orders/${order.id}/confirm-result`); await loadOrders(); toast('已确认回收结果') } catch (e) { toast(apiError(e), true) }
}
async function objectResult(order) {
  const reason = window.prompt('请填写对回收结果的异议')
  if (!reason?.trim()) return
  try { await http.post(`/orders/${order.id}/raise-objection`, { reason }); await loadOrders(); toast('异议已提交管理员处理') } catch (e) { toast(apiError(e), true) }
}
async function approveOrder(order) {
  try { await http.post(`/admin/orders/${order.id}/approve`); await loadOrders(); toast('审核通过，订单待分派') } catch (e) { toast(apiError(e), true) }
}
async function rejectOrder(order) {
  const reason = window.prompt('请输入驳回原因')
  if (!reason?.trim()) return
  try { await http.post(`/admin/orders/${order.id}/reject`, { reason }); await loadOrders(); toast('订单已驳回') } catch (e) { toast(apiError(e), true) }
}
async function handleException(order) {
  const resolution = window.prompt('请输入异常处理结果')
  if (!resolution?.trim()) return
  try { await http.post(`/admin/orders/${order.id}/handle-exception`, { resolution }); await loadOrders(); toast('异常处理完成') } catch (e) { toast(apiError(e), true) }
}
async function handleCancellation(order) {
  const approved = window.confirm('确定同意取消该订单吗？点击“取消”将拒绝取消申请。')
  const comment = window.prompt('请填写处理意见')
  if (!comment?.trim()) return
  try { await http.post(`/admin/orders/${order.id}/handle-cancellation`, { approved, comment }); await loadOrders(); toast(approved ? '订单已取消' : '已拒绝取消') } catch (e) { toast(apiError(e), true) }
}
async function assignOrder(order) {
  if (!data.workers.length) await loadWorkers()
  const workerId = window.prompt(`请输入回收人员ID：\n${data.workers.map((w) => `${w.id}：${w.realName}（${w.username}）`).join('\n')}`)
  if (!workerId) return
  try {
    const action = order.status === 'PENDING_REASSIGNMENT' ? 'reassign' : 'assign'
    await http.post(`/admin/orders/${order.id}/${action}`, { workerId: Number(workerId), comment: '管理员分派回收任务' })
    await loadOrders(); toast('任务已分派')
  } catch (e) { toast(apiError(e), true) }
}
async function createWorker() {
  busy.value = true
  try { await http.post('/admin/workers', workerForm); Object.assign(workerForm, { username: '', password: '', realName: '', phone: '' }); await loadUsers(); toast('回收人员账号已创建') }
  catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}
async function saveCategory() {
  busy.value = true
  try {
    if (categoryForm.id) await http.put(`/admin/categories/${categoryForm.id}`, categoryForm)
    else await http.post('/admin/categories', categoryForm)
    Object.assign(categoryForm, { id: null, name: '', description: '', recycleRequirements: '' }); await loadCategories(true); toast('品类已保存')
  } catch (e) { toast(apiError(e), true) } finally { busy.value = false }
}
function editCategory(c) { Object.assign(categoryForm, { id: c.id, name: c.name, description: c.description || '', recycleRequirements: c.recycleRequirements || '' }) }
async function toggleCategory(c) {
  try { await http.patch(`/admin/categories/${c.id}/status`, { status: c.status === 1 ? 0 : 1 }); await loadCategories(true); toast('品类状态已更新') } catch (e) { toast(apiError(e), true) }
}
async function workerAction(order, action) {
  try {
    if (action === 'accept') await http.post(`/worker/tasks/${order.id}/accept`)
    if (action === 'reject') {
      const reason = window.prompt('请输入拒绝原因'); if (!reason?.trim()) return
      await http.post(`/worker/tasks/${order.id}/reject`, { reason })
    }
    if (action === 'start') await http.post(`/worker/tasks/${order.id}/start`)
    if (action === 'exception') {
      const reason = window.prompt('请描述任务异常'); if (!reason?.trim()) return
      await http.post(`/worker/tasks/${order.id}/exception`, { reason })
    }
    await loadOrders(); toast('任务状态已更新')
  } catch (e) { toast(apiError(e), true) }
}
async function openResult(order) {
  try {
    const detail = responseData(await http.get(`/worker/tasks/${order.id}`))
    selectedOrder.value = detail
    resultForm.remark = ''
    resultForm.items = (detail.items || []).map((item) => ({ categoryId: item.categoryId, categoryName: item.categoryName, actualQuantity: item.estimatedQuantity, actualWeight: 0, weightUnit: 'kg' }))
  } catch (e) { toast(apiError(e), true) }
}
async function openOrderDetail(order) {
  detailOrder.value = null
  detailLoading.value = true
  try {
    const endpoint = user.value.role === 'RESIDENT'
      ? `/orders/${order.id}`
      : user.value.role === 'WORKER'
        ? `/worker/tasks/${order.id}`
        : `/admin/orders/${order.id}`
    detailOrder.value = responseData(await http.get(endpoint))
  } catch (e) { toast(apiError(e), true) } finally { detailLoading.value = false }
}
async function submitResult() {
  if (!selectedOrder.value) return
  try { await http.post(`/worker/tasks/${selectedOrder.value.order.id}/result`, resultForm); selectedOrder.value = null; await loadOrders(); toast('实际回收数量和重量已登记') }
  catch (e) { toast(apiError(e), true) }
}
async function updateProfile() {
  try { await http.put('/users/me', { realName: profile.realName, phone: profile.phone }); await loadMe(); toast('个人资料已保存') } catch (e) { toast(apiError(e), true) }
}
async function changePassword() {
  try { await http.put('/users/me/password', { oldPassword: profile.oldPassword, newPassword: profile.newPassword }); profile.oldPassword = ''; profile.newPassword = ''; toast('密码已修改') } catch (e) { toast(apiError(e), true) }
}
async function updateUserStatus(u) {
  const status = u.status === 1 ? 0 : 1
  try { await http.patch(`/admin/users/${u.id}/status`, { status }); await loadUsers(); toast(status ? '账号已启用' : '账号已停用') } catch (e) { toast(apiError(e), true) }
}
async function createReview(order) {
  reviewOrder.value = order
  reviewForm.rating = 5
  reviewForm.content = ''
}
async function submitReview() {
  if (!reviewOrder.value || reviewSubmitting.value) return
  reviewSubmitting.value = true
  try {
    await http.post(`/orders/${reviewOrder.value.id}/review`, { rating: reviewForm.rating, content: reviewForm.content.trim() })
    reviewOrder.value = null
    toast('评价已提交，感谢你的反馈')
  } catch (e) { toast(apiError(e), true) } finally { reviewSubmitting.value = false }
}
async function refresh() { await loadPage() }

onMounted(async () => {
  if (!token.value) return
  try { await loadMe(); await loadPage() } catch { requireLogin() }
})
</script>

<template>
  <main v-if="!user" class="auth-page">
    <section class="auth-card">
      <div class="auth-brand"><span class="brand-mark">图</span><div><strong>图图社区</strong><small>废旧家电一体化管理系统</small></div></div>
      <p class="eyebrow">COMMUNITY RECYCLING</p>
      <h1>{{ authMode === 'login' ? '欢迎回来' : '创建居民账号' }}</h1>
      <p class="auth-sub">{{ authMode === 'login' ? '登录后管理回收申请与上门服务。' : '居民可自行注册，开启便捷回收服务。' }}</p>
      <form class="form-stack" @submit.prevent="authMode === 'login' ? login() : register()">
        <label>登录账号<input v-model.trim="authForm.username" required maxlength="50" autocomplete="username" placeholder="请输入登录账号" /></label>
        <label>登录密码<input v-model="authForm.password" required minlength="8" type="password" autocomplete="current-password" placeholder="至少 8 位" /></label>
        <template v-if="authMode === 'register'">
          <label>居民姓名<input v-model.trim="authForm.realName" required maxlength="50" placeholder="请输入姓名" /></label>
          <label>联系电话<input v-model.trim="authForm.phone" maxlength="20" placeholder="选填" /></label>
        </template>
        <button class="primary-button full" :disabled="busy">{{ busy ? '请稍候…' : authMode === 'login' ? '登录系统' : '注册居民账号' }}</button>
      </form>
      <button class="auth-switch" @click="authMode = authMode === 'login' ? 'register' : 'login'">{{ authMode === 'login' ? '还没有居民账号？立即注册' : '已有账号？返回登录' }}</button>
      <p class="auth-note">管理员和回收人员账号由管理员创建。</p>
    </section>
    <Transition name="toast"><div v-if="notice" class="toast-message" :class="{ danger: error }">{{ notice }}</div></Transition>
  </main>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="#" @click.prevent="go('工作台')"><span class="brand-mark">图</span><span><strong>图图社区</strong><small>让闲置家电找到新去处</small></span></a>
      <div class="community-pill"><span class="live-dot"></span> 图图社区</div>
      <p class="nav-caption">工作空间</p>
      <nav class="main-nav" aria-label="主导航">
        <button v-for="(item, index) in nav" :key="item" :class="['nav-item', { active: active === item }]" @click="go(item)">
          <span class="nav-icon">{{ ['⌂', '＋', '▤', '⌖', '◇', '✓', '▦'][index % 7] }}</span><span>{{ item }}</span>
          <span v-if="item === '订单审核' && data.orders.filter(o => o.status === 'PENDING_REVIEW').length" class="nav-count">{{ data.orders.filter(o => o.status === 'PENDING_REVIEW').length }}</span>
        </button>
      </nav>
      <section class="sidebar-guide" aria-label="回收服务流程">
        <div class="sidebar-guide-heading"><span>♻</span><small>绿色回收 · 服务流程</small></div>
        <h2>旧家电回收<br />三步轻松完成</h2>
        <div class="sidebar-guide-steps">
          <div><i>01</i><span>在线预约</span></div>
          <div><i>02</i><span>上门回收</span></div>
          <div><i>03</i><span>确认结果</span></div>
        </div>
        <span class="sidebar-guide-decoration">♻</span>
      </section>
      <div class="sidebar-bottom">
        <div class="help-card"><span class="help-icon">?</span><div><strong>图图社区回收服务</strong><small>让旧家电获得新价值</small></div></div>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="topbar-context">
          <span class="topbar-mark">图</span>
          <div class="topbar-context-copy">
            <div class="breadcrumbs"><span>图图社区</span><span class="slash">/</span><strong>{{ pageTitle }}</strong></div>
            <small>预约、上门、确认 · 回收全流程在线管理</small>
          </div>
        </div>
        <div class="top-actions"><span class="role-chip">{{ roleName }}</span><span class="top-divider"></span><button class="top-profile-button" @click="go('个人资料')" title="个人资料"><span class="top-avatar">{{ (user.realName || user.username || '用').slice(0, 1) }}</span><span class="top-user-name">{{ user.realName || user.username }}</span></button><button class="top-logout-button" @click="logout">退出登录</button></div>
      </header>
      <div class="content-wrap">
        <section class="welcome-row"><div><p class="eyebrow">图图社区 · 废旧家电回收服务</p><h1>{{ active === '工作台' ? `你好，${user.realName || user.username}` : active }}<span v-if="active === '工作台'" class="wave">✳</span></h1><p class="subheading">{{ active === '工作台' ? '查看最新回收进度，继续管理社区绿色回收服务。' : '所有数据来自当前后端服务。' }}</p></div><button class="primary-button" @click="refresh" :disabled="busy">↻ 刷新数据</button></section>

        <section v-if="active === '工作台'" class="metric-grid">
          <article class="metric-card"><div class="metric-top"><span class="metric-label">当前可见订单</span><span class="metric-icon lavender">▤</span></div><div class="metric-value">{{ total }}<small> 单</small></div><div class="metric-foot">来自后端实时订单列表</div></article>
          <article class="metric-card"><div class="metric-top"><span class="metric-label">回收品类</span><span class="metric-icon mint">♻</span></div><div class="metric-value">{{ data.categories.length }}<small> 类</small></div><div class="metric-foot">当前可用或已配置品类</div></article>
          <article class="metric-card"><div class="metric-top"><span class="metric-label">待处理事项</span><span class="metric-icon peach">◷</span></div><div class="metric-value">{{ data.orders.filter(o => ['PENDING_REVIEW', 'PENDING_ACCEPTANCE', 'PENDING_RESIDENT_CONFIRMATION'].includes(o.status)).length }}<small> 项</small></div><div class="metric-foot">依当前角色和订单状态统计</div></article>
          <article class="metric-card"><div class="metric-top"><span class="metric-label">服务身份</span><span class="metric-icon butter">♙</span></div><div class="metric-value role-metric">{{ roleName }}</div><div class="metric-foot">{{ user.username }}</div></article>
        </section>

        <section v-if="active === '工作台'" class="quick-section">
          <div class="quick-heading"><div><h2>快捷入口</h2><p>常用操作一步直达</p></div><span class="quick-heading-badge">{{ roleName }} · 常用功能</span></div>
          <div class="quick-grid">
            <button v-for="item in dashboardActions" :key="item.title" class="quick-action" @click="runDashboardAction(item.action)">
              <span :class="['quick-icon', item.tone]">{{ item.icon }}</span>
              <span class="quick-copy"><strong>{{ item.title }}</strong><small>{{ item.hint }}</small></span>
              <span class="quick-arrow">→</span>
            </button>
          </div>
        </section>

        <section v-if="active === '工作台'" class="panel table-panel"><div class="panel-heading"><div><h2>最近订单 / 任务</h2><p>后端实时数据</p></div><button class="text-button" @click="go(user.role === 'RESIDENT' ? '我的订单' : user.role === 'WORKER' ? '我的任务' : '订单管理')">查看全部 →</button></div><OrderTable :orders="displayOrders.slice(0, 6)" :role="user.role" @detail="openOrderDetail" @cancel="cancelOrder" @confirm="confirmResult" @object="objectResult" @review="createReview" @approve="approveOrder" @reject="rejectOrder" @assign="assignOrder" @handle-exception="handleException" @handle-cancellation="handleCancellation" @worker-action="workerAction" @result="openResult" /></section>

        <section v-if="['我的订单', '我的任务', '订单审核', '订单管理'].includes(active)" class="panel table-panel">
          <div class="panel-heading"><div><h2>{{ active }}</h2><p>共 {{ total }} 条记录</p></div><button class="outline-button" @click="refresh">刷新</button></div>
          <div class="table-tools"><label class="search-box"><span>⌕</span><input v-model="filters.keyword" placeholder="搜索订单编号/地址" @keyup.enter="refresh" /></label><select v-model="filters.status" class="select-control" @change="refresh"><option value="">全部状态</option><option v-for="(label, code) in { PENDING_REVIEW:'待审核', PENDING_ASSIGNMENT:'待分派', PENDING_ACCEPTANCE:'待回收人员接单', ACCEPTED:'已接单', IN_PROGRESS:'回收中', PENDING_RESIDENT_CONFIRMATION:'待居民确认', COMPLETED:'已完成', CANCELLED:'已取消', REJECTED:'已驳回', EXCEPTION_PENDING:'异常处理中' }" :key="code" :value="code">{{ label }}</option></select><button class="outline-button" @click="refresh">查询</button></div>
          <OrderTable :orders="displayOrders" :role="user.role" @detail="openOrderDetail" @cancel="cancelOrder" @confirm="confirmResult" @object="objectResult" @review="createReview" @approve="approveOrder" @reject="rejectOrder" @assign="assignOrder" @handle-exception="handleException" @handle-cancellation="handleCancellation" @worker-action="workerAction" @result="openResult" />
          <p class="pagination-note">当前显示 {{ data.orders.length }} 条，列表最多加载 50 条。</p>
        </section>

        <section v-if="active === '提交回收申请'" class="panel form-panel">
          <div class="panel-heading"><div><h2>预约上门回收</h2><p>选择地址、预约时间和需要回收的家电品类</p></div></div>
          <form class="form-grid" @submit.prevent="submitOrder">
            <label>回收地址<select v-model="orderForm.addressId" required><option value="" disabled>请选择地址</option><option v-for="a in data.addresses" :key="a.id" :value="a.id">{{ a.region }} {{ a.detailAddress }}（{{ a.contactName }}）</option></select></label>
            <label>预约上门时间<input v-model="orderForm.appointmentTime" type="datetime-local" required /></label>
            <div class="full-row"><div class="section-label">回收品类与预计数量</div><div v-for="(item, index) in orderForm.items" :key="index" class="item-editor"><select v-model="item.categoryId" required><option value="" disabled>请选择品类</option><option v-for="c in data.categories" :key="c.id" :value="c.id">{{ c.name }}</option></select><input v-model.number="item.estimatedQuantity" type="number" min="1" required placeholder="预计数量"/><input v-model="item.itemDescription" maxlength="500" placeholder="家电情况说明（选填）"/><button type="button" class="icon-remove" @click="removeOrderItem(index)">移除</button></div><button type="button" class="text-button" @click="addOrderItem">＋ 添加另一种品类</button><p v-if="!data.categories.length" class="inline-warning">暂无启用品类，请联系管理员先维护回收品类。</p></div>
            <label class="full-row">补充说明<textarea v-model="orderForm.description" maxlength="1000" rows="3" placeholder="描述家电情况、搬运条件等（选填）"></textarea></label>
            <div class="full-row form-actions"><button class="primary-button" :disabled="busy || !data.categories.length">提交回收申请</button></div>
          </form>
        </section>

        <section v-if="active === '回收地址'" class="two-column">
          <article class="panel form-panel"><div class="panel-heading"><div><h2>{{ addressEditId ? '编辑回收地址' : '新增回收地址' }}</h2><p>订单会保存地址快照</p></div><button v-if="addressEditId" type="button" class="outline-button" @click="addressEditId=null;Object.assign(addressForm,{contactName:'',contactPhone:'',region:'',detailAddress:'',addressRemark:'',isDefault:true})">取消编辑</button></div><form class="form-stack" @submit.prevent="saveAddress"><label>联系人<input v-model.trim="addressForm.contactName" required maxlength="50" /></label><label>联系电话<input v-model.trim="addressForm.contactPhone" required maxlength="20" /></label><label>所在区域<input v-model.trim="addressForm.region" required maxlength="150" placeholder="如：图图社区 1 号楼" /></label><label>详细地址<input v-model.trim="addressForm.detailAddress" required maxlength="255" placeholder="如：1 单元 101 室" /></label><label>地址备注<input v-model="addressForm.addressRemark" maxlength="255" /></label><label class="checkbox-line"><input v-model="addressForm.isDefault" type="checkbox" /> 设为默认地址</label><button class="primary-button">{{ addressEditId ? '保存修改' : '保存地址' }}</button></form></article>
          <article class="panel"><div class="panel-heading"><div><h2>我的地址</h2><p>{{ data.addresses.length }} 条</p></div><button class="outline-button" @click="loadAddresses">刷新</button></div><div v-for="a in data.addresses" :key="a.id" class="address-card"><div class="address-head"><strong>{{ a.contactName }}</strong><span>{{ a.contactPhone }}</span><em v-if="a.isDefault === 1">默认</em></div><p>{{ a.region }} {{ a.detailAddress }}</p><small>地址 ID：{{ a.id }}<span v-if="a.addressRemark"> · {{ a.addressRemark }}</span></small><div class="inline-actions address-actions"><button class="text-button" @click="editAddress(a)">编辑</button><button v-if="a.isDefault !== 1" class="text-button" @click="setDefaultAddress(a)">设为默认</button><button class="text-button danger-text" @click="deleteAddress(a)">删除</button></div></div><div v-if="!data.addresses.length" class="empty-state">还没有保存回收地址</div></article>
        </section>

        <section v-if="active === '回收品类' && user.role !== 'ADMIN'" class="panel"><div class="panel-heading"><div><h2>可回收品类</h2><p>请选择系统当前开放的品类</p></div></div><div class="category-grid"><article v-for="c in data.categories" :key="c.id" class="category-card"><span class="metric-icon mint">♻</span><h3>{{ c.name }}</h3><p>{{ c.description || '暂无说明' }}</p><small>{{ c.recycleRequirements || '暂无回收要求' }}</small></article></div><div v-if="!data.categories.length" class="empty-state">暂未开放回收品类</div></section>

        <section v-if="active === '回收品类' && user.role === 'ADMIN'" class="two-column">
          <article class="panel form-panel"><div class="panel-heading"><div><h2>{{ categoryForm.id ? '编辑品类' : '新增回收品类' }}</h2><p>管理居民可选择的家电回收品类</p></div></div><form class="form-stack" @submit.prevent="saveCategory"><label>品类名称<input v-model.trim="categoryForm.name" required maxlength="100" /></label><label>品类说明<textarea v-model="categoryForm.description" maxlength="500" rows="3"></textarea></label><label>回收要求<textarea v-model="categoryForm.recycleRequirements" maxlength="500" rows="3"></textarea></label><div class="inline-actions"><button class="primary-button">保存品类</button><button v-if="categoryForm.id" type="button" class="outline-button" @click="Object.assign(categoryForm,{id:null,name:'',description:'',recycleRequirements:''})">取消编辑</button></div></form></article>
          <article class="panel"><div class="panel-heading"><div><h2>品类列表</h2><p>{{ data.categories.length }} 项</p></div></div><div v-for="c in data.categories" :key="c.id" class="simple-row"><div><strong>{{ c.name }}</strong><small>{{ c.description || '暂无说明' }} · {{ c.status === 1 ? '启用' : '停用' }}</small></div><button class="text-button" @click="editCategory(c)">编辑</button><button class="text-button danger-text" @click="toggleCategory(c)">{{ c.status === 1 ? '停用' : '启用' }}</button></div></article>
        </section>

        <section v-if="active === '用户管理'" class="two-column">
          <article class="panel form-panel"><div class="panel-heading"><div><h2>创建回收人员</h2><p>回收人员通过管理员创建账号</p></div></div><form class="form-stack" @submit.prevent="createWorker"><label>登录账号<input v-model.trim="workerForm.username" required maxlength="50" /></label><label>初始密码<input v-model="workerForm.password" type="password" required minlength="8" /></label><label>姓名<input v-model.trim="workerForm.realName" required maxlength="50" /></label><label>联系电话<input v-model.trim="workerForm.phone" maxlength="20" /></label><button class="primary-button">创建回收人员</button></form></article>
          <article class="panel"><div class="panel-heading"><div><h2>用户列表</h2><p>{{ data.users.length }} 人</p></div><button class="outline-button" @click="loadUsers">刷新</button></div><div v-for="u in data.users" :key="u.id" class="simple-row"><div><strong>{{ u.realName || u.username }} <small class="role-tag">{{ { RESIDENT:'居民', WORKER:'回收人员', ADMIN:'管理员' }[u.role] || u.role }}</small></strong><small>ID {{ u.id }} · {{ u.username }} · {{ u.phone || '未填写电话' }}</small></div><button v-if="u.role !== 'ADMIN'" class="text-button" @click="updateUserStatus(u)">{{ u.status === 1 ? '停用账号' : '启用账号' }}</button><span :class="['status-badge', u.status === 1 ? 'green' : 'muted']">{{ u.status === 1 ? '启用' : '停用' }}</span></div></article>
        </section>

        <section v-if="active === '业务统计'" class="panel"><div class="panel-heading"><div><h2>业务统计</h2><p>基于后端统计接口</p></div><button class="outline-button" @click="loadStats">刷新统计</button></div><div v-if="data.stats" class="stats-grid"><div class="stat-box"><small>订单总数</small><strong>{{ data.stats.overview?.totalOrders ?? 0 }}</strong></div><div class="stat-box"><small>累计回收重量</small><strong>{{ data.stats.weight?.totalWeight ?? 0 }} <em>{{ data.stats.weight?.weightUnit || 'kg' }}</em></strong></div><div v-for="(count, status) in (data.stats.overview?.statusCounts || {})" :key="status" class="stat-box"><small>{{ statusText(status) }}</small><strong>{{ count }}</strong></div></div><div class="simple-row" v-for="c in (data.stats?.categories || [])" :key="c.categoryId"><div><strong>{{ c.categoryName }}</strong><small>实际回收数量</small></div><b>{{ c.actualQuantity }} 件</b></div></section>

        <section v-if="active === '服务评价'" class="panel"><div class="panel-heading"><div><h2>服务评价</h2><p>居民确认订单完成后提交的评价</p></div><button class="outline-button" @click="loadReviews">刷新</button></div><div v-for="r in data.reviews" :key="r.id" class="simple-row"><div><strong>订单 {{ r.orderId }} <span class="review-stars">{{ '★'.repeat(r.rating) }}{{ '☆'.repeat(5-r.rating) }}</span></strong><small>居民 ID：{{ r.residentId }} · {{ prettyTime(r.createdAt) }}</small><p class="review-content">{{ r.content || '居民未填写文字评价' }}</p></div></div><div v-if="!data.reviews.length" class="empty-state">暂无服务评价</div></section>

        <section v-if="active === '个人资料'" class="two-column"><article class="panel form-panel"><div class="panel-heading"><div><h2>个人资料</h2><p>账号：{{ user.username }} · {{ roleName }}</p></div></div><form class="form-stack" @submit.prevent="updateProfile"><label>姓名<input v-model.trim="profile.realName" required maxlength="50" /></label><label>联系电话<input v-model.trim="profile.phone" maxlength="20" /></label><button class="primary-button">保存资料</button></form></article><article class="panel form-panel"><div class="panel-heading"><div><h2>修改密码</h2><p>新密码长度为 8 至 72 位</p></div></div><form class="form-stack" @submit.prevent="changePassword"><label>原密码<input v-model="profile.oldPassword" type="password" required /></label><label>新密码<input v-model="profile.newPassword" type="password" required minlength="8" maxlength="72" /></label><button class="outline-button">修改密码</button></form></article></section>

        <footer class="footer"><span>© 2026 图图社区 · 废旧家电回收管理系统</span><span>{{ busy ? '正在同步数据…' : '数据已连接后端服务' }} <b>✳</b></span></footer>
      </div>
    </main>

    <div v-if="detailLoading" class="drawer-backdrop detail-loading-backdrop"><div class="detail-loading-card"><span class="loading-spinner"></span><strong>正在加载订单详情</strong></div></div>
    <div v-if="detailOrder" class="drawer-backdrop" @click.self="detailOrder = null">
      <aside class="order-drawer" role="dialog" aria-modal="true" aria-label="订单详情">
        <header class="drawer-header">
          <div><p class="eyebrow">订单详情</p><h2>{{ detailOrder.order?.orderNo || '订单' }}</h2><span :class="['status-badge', statusClass(detailOrder.order?.status)]"><i></i>{{ statusText(detailOrder.order?.status) }}</span></div>
          <button class="drawer-close" aria-label="关闭订单详情" @click="detailOrder = null">×</button>
        </header>

        <div class="drawer-body">
          <section class="detail-section progress-section">
            <div class="detail-section-title"><h3>订单进度</h3><small>创建于 {{ prettyTime(detailOrder.order?.createdAt) }}</small></div>
            <div v-if="['CANCELLED', 'REJECTED'].includes(detailOrder.order?.status)" class="terminal-notice" :class="detailOrder.order?.status === 'CANCELLED' ? 'cancelled' : 'rejected'">
              <strong>{{ statusText(detailOrder.order?.status) }}</strong><span>{{ detailOrder.order?.cancelReason || (detailOrder.order?.status === 'REJECTED' ? '订单未通过管理员审核' : '订单已取消') }}</span>
            </div>
            <div v-else class="progress-list">
              <div v-for="(step, index) in orderProgress" :key="step.title" :class="['progress-step', { done: index < progressIndex(detailOrder.order?.status), current: index === progressIndex(detailOrder.order?.status) }]">
                <span class="progress-dot">{{ index < progressIndex(detailOrder.order?.status) ? '✓' : index + 1 }}</span>
                <div><strong>{{ step.title }}</strong><small>{{ step.description }}</small></div>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title"><h3>预约与联系信息</h3></div>
            <div class="detail-info-grid">
              <div><small>预约上门时间</small><strong>{{ prettyTime(detailOrder.order?.appointmentTime) }}</strong></div>
              <div><small>联系人</small><strong>{{ detailOrder.order?.contactName || '—' }}</strong></div>
              <div><small>联系电话</small><strong>{{ detailOrder.order?.contactPhone || '—' }}</strong></div>
              <div class="detail-info-wide"><small>回收地址</small><strong>{{ detailOrder.order?.region }} {{ detailOrder.order?.detailAddress }}</strong><em v-if="detailOrder.order?.addressRemark">{{ detailOrder.order.addressRemark }}</em></div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title"><h3>申报回收品类</h3><small>{{ detailOrder.items?.length || 0 }} 项</small></div>
            <div v-for="item in detailOrder.items || []" :key="item.id" class="detail-item-row"><span class="detail-item-icon">♻</span><div><strong>{{ item.categoryName }}</strong><small>{{ item.itemDescription || '未填写家电情况说明' }}</small></div><b>{{ item.estimatedQuantity }} 件</b></div>
            <p v-if="detailOrder.order?.description" class="detail-note"><small>补充说明</small>{{ detailOrder.order.description }}</p>
          </section>

          <section v-if="detailOrder.resultItems?.length" class="detail-section">
            <div class="detail-section-title"><h3>实际回收结果</h3><small>{{ prettyTime(detailOrder.recycleRecord?.recordedAt) }}</small></div>
            <div v-for="item in detailOrder.resultItems" :key="item.id" class="detail-item-row result-detail-row"><span class="detail-item-icon actual">✓</span><div><strong>{{ item.categoryName }}</strong><small>实际回收数量</small></div><b>{{ item.actualQuantity }} 件 <span>·</span> {{ item.actualWeight }} {{ item.weightUnit || 'kg' }}</b></div>
            <p v-if="detailOrder.recycleRecord?.remark" class="detail-note"><small>回收备注</small>{{ detailOrder.recycleRecord.remark }}</p>
          </section>

          <section class="detail-section">
            <div class="detail-section-title"><h3>状态变更记录</h3><small>{{ detailOrder.logs?.length || 0 }} 条</small></div>
            <div v-if="detailOrder.logs?.length" class="log-list">
              <div v-for="log in detailOrder.logs" :key="log.id" class="log-entry"><span class="log-dot"></span><div><strong>{{ logActionText(log.action) }}</strong><small>{{ prettyTime(log.createdAt) }}</small><p v-if="log.comment">{{ log.comment }}</p></div></div>
            </div>
            <div v-else class="detail-empty">暂无状态变更记录</div>
          </section>
        </div>
        <footer class="drawer-footer"><span>订单编号：{{ detailOrder.order?.orderNo }}</span><button class="outline-button" @click="detailOrder = null">关闭</button></footer>
      </aside>
    </div>

    <div v-if="selectedOrder" class="modal-backdrop" @click.self="selectedOrder = null"><section class="modal-card"><div class="panel-heading"><div><h2>登记实际回收结果</h2><p>订单 {{ selectedOrder.order.orderNo }} · 按品类登记数量和重量</p></div><button class="icon-remove" @click="selectedOrder = null">关闭</button></div><form class="form-stack" @submit.prevent="submitResult"><div v-for="(item, index) in resultForm.items" :key="index" class="result-item"><strong>{{ item.categoryName }}</strong><label>实际数量<input v-model.number="item.actualQuantity" type="number" min="0" required /></label><label>实际重量（kg）<input v-model.number="item.actualWeight" type="number" min="0" step="0.01" required /></label></div><label>备注<textarea v-model="resultForm.remark" rows="2" maxlength="500"></textarea></label><button class="primary-button">提交回收结果</button></form></section></div>
    <div v-if="reviewOrder" class="review-backdrop" @click.self="reviewOrder = null">
      <section class="review-modal" role="dialog" aria-modal="true" aria-labelledby="review-title">
        <header class="review-header"><span class="review-mark">★</span><button class="review-close" aria-label="关闭评价" @click="reviewOrder = null">×</button><p class="eyebrow">图图社区 · 服务反馈</p><h2 id="review-title">评价本次回收服务</h2><p>你的反馈会帮助我们持续改进上门回收体验。</p></header>
        <form class="review-form" @submit.prevent="submitReview">
          <div class="review-order-ref"><span>评价订单</span><strong>{{ reviewOrder.orderNo }}</strong></div>
          <fieldset class="rating-field"><legend>整体服务满意度</legend><div class="rating-picker">
            <button v-for="score in 5" :key="score" type="button" :class="['rating-star', { selected: score <= reviewForm.rating }]" :aria-label="`${score} 星`" :aria-pressed="reviewForm.rating === score" @click="reviewForm.rating = score">★</button>
          </div><div class="rating-caption"><strong>{{ ['', '很不满意', '不太满意', '一般', '满意', '非常满意'][reviewForm.rating] }}</strong><span>{{ reviewForm.rating }} / 5</span></div></fieldset>
          <label class="review-comment-label" for="review-comment">说说你的体验 <span>选填</span></label>
          <textarea id="review-comment" v-model="reviewForm.content" maxlength="500" rows="4" placeholder="例如：预约方便、回收人员准时、服务态度很好……"></textarea>
          <div class="review-char-count">{{ reviewForm.content.length }} / 500</div>
          <div class="review-actions"><button type="button" class="outline-button" :disabled="reviewSubmitting" @click="reviewOrder = null">暂不评价</button><button type="submit" class="primary-button" :disabled="reviewSubmitting">{{ reviewSubmitting ? '正在提交…' : '提交评价' }}</button></div>
        </form>
      </section>
    </div>
    <Transition name="toast"><div v-if="notice" class="toast-message" :class="{ danger: error }">{{ notice }}</div></Transition>
  </div>
</template>
