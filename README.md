# 图图社区废旧家电一体化管理系统

基于 Spring Boot 和 Vue 3 的社区废旧家电回收管理系统，包含居民、回收人员和管理员使用的业务功能。

## 项目结构

```text
backend/    Spring Boot 后端、MySQL 建表脚本
frontend/   Vue 3 + Vite 前端
outputs/    需求、设计、数据库、接口、测试文档及开题答辩 PPT
```

## 技术栈

- 后端：Java 8、Spring Boot 2.7.18、Spring Security、JWT、MyBatis-Plus、MySQL 8
- 前端：Vue 3、Vite、Axios
- API 文档：Swagger UI；OpenAPI 文件位于 `outputs/openapi.yaml`

## 快速启动

### 1. 初始化数据库

创建 MySQL 数据库 `tutu_recycling`，并导入 `backend/src/main/resources/db/schema.sql`。数据库连接参数通过 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 环境变量配置。

### 2. 配置并启动后端

复制 `backend/src/main/resources/application-example.yml` 为 `application.yml`，按本机环境设置数据库密码和 JWT 密钥。`application.yml` 是本地配置文件，已加入 Git 忽略规则。

在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

Swagger UI：<http://localhost:8080/swagger-ui.html>

首次初始化管理员时，可在启动后端前设置 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`；密码至少 8 位。管理员创建后可移除这两个环境变量。JWT 密钥通过 `APP_JWT_SECRET` 设置。

### 3. 启动前端

在 `frontend` 目录运行：

```bash
npm ci
npm run dev
```

如后端 API 地址不是默认值，可参考 `.env.example` 创建本地 `.env` 并修改 `VITE_API_BASE_URL`。

## 项目文档

需求、总体设计、用例与流程、数据库设计、接口说明、测试用例和开题答辩材料均位于 `outputs/`。
