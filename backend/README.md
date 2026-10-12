# 图图社区废旧家电管理系统后端

后端使用 Spring Boot 2.7.18、Java 8、MyBatis-Plus 和 MySQL。当前项目骨架已包含居民注册登录、JWT 身份校验、地址与品类维护、订单审核分派、回收人员任务处理、回收结果确认、评价及基础统计接口。

## 环境

- JDK 8 或更高版本；当前依赖 Spring Boot 2.7.18 是为兼容工作区检测到的 JDK 8。
- Maven 3.5 或更高版本。
- MySQL 8.0。

Spring Boot 2.7.18 官方系统要求支持 Java 8；MyBatis-Plus 官方提供了 Spring Boot 2 starter。

## 初始化数据库

1. 创建数据库：

   ```sql
   CREATE DATABASE tutu_recycling DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. 将 `src/main/resources/db/schema.sql` 导入 `tutu_recycling`。
3. 设置数据库连接环境变量，或修改本地 `application.yml` 中的默认连接值。

如果已安装 Docker Compose，也可以在 `backend` 目录运行 `docker compose up -d mysql`。初始化脚本只会在 MySQL 数据卷首次创建时执行。

环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。

## 首次创建管理员

首次启动时，可设置 `BOOTSTRAP_ADMIN_USERNAME` 和 `BOOTSTRAP_ADMIN_PASSWORD`。管理员不存在时，应用会以 BCrypt 哈希保存该账号。请使用至少 8 位的密码；不要将真实密码提交到代码仓库。

JWT 密钥通过 `APP_JWT_SECRET` 设置。当前配置中的默认值仅适用于本机开发，部署前应替换为随机密钥。

PowerShell 示例（在启动后端的同一终端设置）：

```powershell
$env:BOOTSTRAP_ADMIN_USERNAME = 'admin'
$env:BOOTSTRAP_ADMIN_PASSWORD = '请替换为至少8位的本地密码'
$env:APP_JWT_SECRET = '请替换为足够长的随机密钥'
mvn spring-boot:run
```

## 启动

在 `backend` 目录运行：

```bash
mvn spring-boot:run
```

如果 Maven 依赖首次下载，项目会将本地缓存放在工作区 `work/maven-repo`。

Swagger UI：`http://localhost:8080/swagger-ui.html`。

## 当前实现范围

- 居民注册、登录、退出及个人资料和密码修改。
- 地址新增、查询、修改、逻辑删除和默认地址设置。
- 回收品类查询及管理员维护。
- 订单创建、居民取消、管理员审核和分派、回收人员确认/拒绝、进度更新和结果登记、居民确认/异议处理。
- 服务评价、管理员用户管理及基础回收统计。

订单接口根据现有需求实现基本状态校验；部署前仍需配置实际数据库、初始管理员和 JWT 密钥。接口文档 `../outputs/openapi.yaml` 是设计草案，服务实现变动时应同步更新。
