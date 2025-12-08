# 🤝 贡献指南 | Contributing Guide

首先，非常感谢你对 **OrangeTools (橙子工具箱)** 感兴趣！🍊

OrangeTools 是一个开源的校园生活工具平台。无论你是想修复一个 Bug、增加一个新功能，还是仅仅修改了一处文档错误，我们都热烈欢迎！

这份文档旨在帮助你快速上手参与开发，并了解我们的协作流程。

## 🌟 在开始之前

我们希望 OrangeTools 是一个**友好、包容**的社区。
* 请保持礼貌和尊重。
* 这是一个学生项目，大家都是在学习中成长，不要害怕犯错。

---

## 🚀 如何参与贡献？

你可以通过以下几种方式参与：

1.  **提交 Issue (反馈问题)**：发现 Bug 或有新功能建议？请先搜索现有的 Issues，如果没有重复，欢迎新建一个 Issue。
2.  **领取任务**：查看 [Issues 列表](https://github.com/FangSuYu/OrangeTools/issues)，寻找带有 `good first issue` (新手友好) 或 `help wanted` 标签的任务。
3.  **提交代码 (Pull Request)**：如果你有能力直接修复问题或开发功能，请直接提交 PR！

---

## 🛠️ 开发流程 (Workflow)

为了保证代码库的整洁和稳定，请遵循以下 GitHub Flow：

### 1. Fork 本仓库
点击右上角的 **Fork** 按钮，将项目复制到你自己的 GitHub 账号下。

### 2. 克隆到本地

```bash
git clone git@github.com:你的用户名/OrangeTools.git
cd OrangeTools
```

### 3. 创建分支 (Branch)

**⚠️以此为戒：请不要在 `main` 分支直接修改！** 所有开发工作请基于 `dev` 分支（如果存在）或 `main` 分支创建一个新的特性分支。

```bash
# 切换到开发分支
git checkout dev

# 创建并切换到你的功能分支
# 命名规范：feat/功能名 或 fix/bug名
git checkout -b feat/add-score-query
```

### 4. 本地开发与测试

请参考 [README.md](https://www.google.com/search?q=./README.md) 中的“快速开始”部分配置本地环境。 确保你的修改在本地运行正常，没有报错。

### 5. 提交代码 (Commit)

我们需要规范的 Commit Message，以便自动生成更新日志。请遵循 **[Conventional Commits](https://www.conventionalcommits.org/)** 规范：

格式：`类型(范围): 描述`

- **feat**: 新功能 (feature)
- **fix**: 修复 Bug
- **docs**: 文档修改
- **style**: 代码格式修改 (不影响逻辑)
- **refactor**: 代码重构
- **chore**: 构建过程或辅助工具的变动

**✅ 正确示例：**

> feat(course): 新增课表导出为图片功能` `fix(login): 修复学号包含特殊字符无法注册的问题

### 6. 推送到你的仓库

> git push origin feat/add-score-query

### 7. 提交 Pull Request (PR)

1. 回到 GitHub 上你的仓库页面。
2. 点击 **"Compare & pull request"**。
3. **目标分支 (Base)**：请选择 `dev` 分支（如果项目目前只有 main，则选 main）。
4. **描述**：请清晰地描述你做了什么修改，如果关联了 Issue，请在描述中写上 `Closes #IssueID`。

## 🎨 代码规范 (Code Style)

为了保持代码风格统一，请注意：

### 前端 (Frontend)

- 使用 **Vue 3 Composition API (`<script setup>`)**。
- 组件命名采用 **大驼峰** (e.g., `UserCard.vue`)。
- 尽量使用 `Element Plus` 提供的组件，保持 UI 统一。
- 不要在代码中保留 `console.log`。

### 后端 (Backend)

- 遵循阿里巴巴 Java 开发手册规范。
- 接口命名需清晰，Result 统一封装。
- 注释要清晰，特别是复杂的业务逻辑。

## 🏆 贡献者荣誉墙

你的代码一旦被合并，你将会被我们添加到 **[贡献者荣誉墙](orangetools.cn/community/contributors)** 上！

这也将成为你简历上的一笔宝贵经历。✨

## ❓ 需要帮助？

如果你在配置环境或阅读代码时遇到困难，可以通过以下方式联系：

- 在 Issue 下留言提问。
- 发送邮件至：[632578328@qq.com]

再次感谢你的贡献！让我们一起把 OrangeTools 变得更好！🍊





