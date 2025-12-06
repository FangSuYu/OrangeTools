# 🍊 OrangeTools (橙子工具箱)

![License](https://img.shields.io/badge/License-GPL%20v3.0-blue.svg)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot%203.5-green.svg)
![Vue3](https://img.shields.io/badge/Frontend-Vue%203.5-42b883.svg)
![Element Plus](https://img.shields.io/badge/UI-Element%20Plus-409eff.svg)
![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)

> **为重庆城市职业学院（城职）学子打造的开源校园生活工具箱。**  致力于用计算机技术解决校园生活中的痛点，海纳百川，服务同学。

## 📖 项目简介 | Introduction

**OrangeTools** 是一个前后端分离的开源项目，旨在构建一个可扩展的校园工具平台。
它不仅是一个工具集合，更是一个标准的**模块化单体 (Modular Monolith)** 架构示例，适合计算机专业学生学习企业级开发规范。

**当前版本：** `v0.1.0 (基石版)`
- ✅ 完整的用户认证体系 (JWT + Spring Security)
- ✅ 规范的前后端工程架构 (Monorepo)
- ✅ 现代化 UI 交互 (Vue3 + Element Plus + 暗黑模式)

## 🛠️ 技术栈 | Tech Stack

### 🎨 前端 (Frontend)
- **核心框架**：Vue 3.5 + Vite 7
- **UI 组件库**：Element Plus (按需加载)
- **状态管理**：Pinia + pinia-plugin-persistedstate (持久化)
- **路由管理**：Vue Router 4 (含动态路由守卫)
- **样式处理**：Sass (SCSS) + CSS Variables (支持一键换肤/暗黑模式)
- **网络请求**：Axios (深度封装拦截器)
- **动画效果**：Vue3-Lottie + View Transitions API

### ☕ 后端 (Backend)
- **核心框架**：Spring Boot 3.5.8
- **ORM 框架**：MyBatis-Plus
- **安全框架**：Spring Security 6
- **鉴权机制**：JWT (JSON Web Token)
- **工具库**：Hutool, Lombok
- **数据库**：MySQL 8.0

---

## 🚀 快速开始 | Quick Start

本项目采用 **Monorepo** 结构，包含 `backend` 和 `frontend` 两个目录。

### 1. 环境准备
- **JDK**: 17+
- **Node.js**: 18+ (推荐 20 LTS)
- **MySQL**: 8.0+
- **包管理器**: pnpm (推荐) 或 npm

### 2. 后端启动 (Backend)

1. 进入后端目录：`cd backend`
2. 配置数据库：
    - 创建数据库 `orange_tools`
    - 将 `src/main/resources/application-dev.yml.example` 重命名为 `application-dev.yml`
    - 修改其中的内容
3. 运行项目：
    - 使用 IDEA 打开 `backend` 目录，运行 `BackendApplication.java`
    - 或命令行：`mvn spring-boot:run`

### 3. 前端启动 (Frontend)

1. 进入前端目录：`cd frontend`
2. 安装依赖：`pnpm install`
3. 启动开发服：`pnpm dev`
4. 访问地址：`http://localhost:5173`

---

## 📂 目录结构 | Directory Structure

```text
OrangeTools/
├── backend/                # 后端工程
│   ├── src/main/java/cn/orangetools/
│   │   ├── common/         # 公共组件 (Result, Utils, Exception)
│   │   ├── system/         # 系统模块 (User, Auth, Role)
│   │   └── modules/        # 业务模块 (各种小工具插件存放处)
│   └── src/main/resources/ # 配置文件
├── frontend/               # 前端工程
│   ├── src/
│   │   ├── api/            # 接口管理
│   │   ├── layout/         # 布局架构 (Sidebar, Navbar)
│   │   ├── stores/         # 状态管理
│   │   ├── styles/         # 样式中心 (Theme, DarkMode)
│   │   └── views/          # 页面视图
│   │       ├── auth/       # 登录注册
│   │       ├── dashboard/  # 主控台
│   │       └── tools/      # 工具视图存放处
│   └── .env.development    # 环境变量
└── README.md               # 说明文档
```
## 🤝 贡献指南 | Contributing
非常欢迎学弟学妹们参与贡献！

1. Fork 本仓库

2. 新建 Feat_xxx 分支

3. 提交代码

4. 新建 Pull Request

## 📄 开源协议 | License
本项目采用 GPL-3.0 协议，即：

✅ 你可以免费使用、修改、分发。

⚠️ 如果你修改了代码并发布，你的修改版也必须开源。

🛡️ 禁止闭源商用。

## 特别鸣谢 / Special Thanks

Developer: [YuHeng https://github.com/FangSuYu]

AI Co-pilot: Gemini