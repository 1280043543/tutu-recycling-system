# 图图社区回收管理前端

基于 Vue 3、Vite 和 Axios，连接同级 `backend` 下的 Spring Boot 服务。

## 启动

先在 IntelliJ IDEA 启动后端（默认 `http://localhost:8080`），再在 VS Code 打开 `frontend` 目录并运行：

```bash
npm install
npm run dev
```

打开终端显示的 Vite 地址，默认是 `http://localhost:5173`。开发服务器已将 `/api` 请求代理到 `http://localhost:8080`，无需额外设置 CORS 地址。

如后端不在本机 8080 端口，可在 `frontend` 目录创建 `.env.local` 并设置：

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
```

修改 Vite 配置或环境变量后需要重启 `npm run dev`。

## 已接入功能

- 居民注册、登录、个人资料、修改密码。
- 居民地址增删改、默认地址设置；品类查询、创建订单、取消订单、确认或异议、完成评价。
- 管理员订单审核、驳回、分派和重新分派；异常及取消处理；居民/回收人员账号状态管理；回收人员创建；品类维护；服务评价查看；业务统计。
- 回收人员任务接单、拒绝、开始回收、异常上报，以及按品类登记实际数量和重量（kg）。

系统根据登录账号角色显示相应导航。居民可自行注册；管理员和回收人员账号由管理员创建。
