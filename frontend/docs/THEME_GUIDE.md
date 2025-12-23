# 前端工程化：明亮/暗黑模式适配规范文档

## 1. 核心原则 (Core Principles)

为了保证应用在明亮 (Light) 和暗黑 (Dark) 模式下都能完美显示，我们在开发新功能时必须遵循以下原则：

*   **禁止硬编码颜色**：严禁在 CSS/SCSS 中直接使用 `#ffffff`, `#333333`, `rgb(0,0,0)` 等固定颜色值。
*   **使用 CSS 变量**：所有涉及颜色的地方（背景、文字、边框、阴影），必须使用我们定义好的 **语义化 CSS 变量**。
*   **依赖 Element Plus 变量**：对于组件库相关的颜色（如主色调、成功/警告/危险色、表单背景等），优先使用 Element Plus 自带的变量，它们会自动适配暗黑模式。

## 2. 常用变量速查表 (Variable Cheat Sheet)

我们在 `src/styles/theme.scss` 中定义了一套全局语义变量，并在 `src/main.js` 中引入了 Element Plus 的暗黑模式变量。

### 2.1 背景色 (Backgrounds)

| 变量名 | 说明 | 适用场景 |
| :--- | :--- | :--- |
| `--bg-color-page` | 页面大背景 | 整个网页的底色 (`body`, `.app-container`) |
| `--bg-color-card` | 卡片背景 | 内容卡片、侧边栏、顶部栏 |
| `--bg-color-overlay` | 浮层背景 | 弹窗、下拉菜单、拖拽时的浮层 |
| `--bg-color-hover` | 悬停背景 | 列表项 Hover、按钮 Hover |

### 2.2 文字颜色 (Text Colors)

| 变量名 | 说明 | 适用场景 |
| :--- | :--- | :--- |
| `--text-color-primary` | 主要文字 | 标题、重要正文 |
| `--text-color-regular` | 常规文字 | 普通正文、表单文字 |
| `--text-color-secondary` | 次要文字 | 辅助说明、元数据、日期 |
| `--text-color-placeholder` | 占位文字 | Input placeholder |

### 2.3 边框与阴影 (Borders & Shadows)

| 变量名 | 说明 | 适用场景 |
| :--- | :--- | :--- |
| `--border-color` | 标准边框 | 卡片边框、分割线 |
| `--border-color-light` | 浅边框 | 内部细微分割线 |
| `--shadow-color` | 阴影颜色 | 卡片阴影、浮层阴影 (暗黑模式下会加深) |

### 2.4 功能色 (Functional Colors - Element Plus)

这些变量由 Element Plus 提供，会自动根据主题变化，无需我们手动维护：

*   **主色**: `var(--el-color-primary)` (以及 `light-3`, `light-9` 等变体)
*   **成功**: `var(--el-color-success)` (淡背景用 `var(--el-color-success-light-9)`)
*   **警告**: `var(--el-color-warning)`
*   **危险**: `var(--el-color-danger)`
*   **填充**: `var(--el-fill-color)`, `var(--el-fill-color-light)` (用于灰色背景块)

## 3. 实战示例 (Examples)

### 3.1 错误示范 (Don't)

```scss
.my-card {
  background-color: #ffffff; /* ❌ 错误：暗黑模式下会刺眼 */
  color: #333333;            /* ❌ 错误：暗黑模式下看不清 */
  border: 1px solid #e0e0e0; /* ❌ 错误 */
}
```

### 3.2 正确示范 (Do)

```scss
.my-card {
  background-color: var(--bg-color-card); /* ✅ 正确 */
  color: var(--text-color-primary);       /* ✅ 正确 */
  border: 1px solid var(--border-color);  /* ✅ 正确 */
  
  /* 如果需要一个淡橙色背景 */
  .highlight {
    background-color: var(--el-color-primary-light-9); /* ✅ 自动适配 */
    color: var(--el-color-primary);
  }
}
```

## 4. 公共部分抽离 (Extraction)

1.  **样式定义**：所有核心变量都集中在 `src/styles/theme.scss`。如果你需要修改“暗黑模式下的卡片背景色”，只需要修改这一个文件，全站都会生效。
2.  **组件库适配**：Element Plus 的适配逻辑在 `src/main.js` 中通过 `import 'element-plus/theme-chalk/dark/css-vars.css'` 统一引入。
3.  **切换逻辑**：切换开关组件封装在 `src/components/ThemeSwitch` 中，通过 VueUse 的 `useDark()` 钩子统一管理 `html.dark` 类名。

## 5. 特殊情况处理 (Edge Cases)

如果遇到 **极个别** 需要在暗黑模式下显示完全不同样式的场景（CSS变量无法满足），可以使用 `:deep(html.dark) &` 选择器，但请慎用：

```scss
.special-box {
  background-image: url('day.png');
  
  /* 仅在暗黑模式下切换背景图 */
  :deep(html.dark) & {
    background-image: url('night.png');
  }
}
```
