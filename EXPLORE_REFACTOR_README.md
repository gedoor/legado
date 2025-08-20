# 新版发现界面改造说明

## 概述
本次改造对阅读APP的发现界面进行了全面升级，增加了书源类型筛选、当前书源管理、分组和分类筛选等功能，提升了用户体验和功能完整性。

## 主要改造内容

### 1. 顶部书源类型切换
- **功能**：在ExploreFragment顶部增加了TabLayout，支持小说/漫画/音频/文件四种书源类型切换
- **实现**：通过BookSource.bookSourceType字段区分不同类型
- **位置**：`app/src/main/res/layout/fragment_explore.xml` - `tab_source_type`
- **代码**：`ExploreFragment.initSourceTypeTabs()`

### 2. 当前书源信息与切换
- **功能**：在界面顶部区域动态展示当前书源名称、分组等信息，提供"切换书源"按钮
- **实现**：弹出书源选择列表，复用BookSource数据结构和相关Dialog
- **位置**：`app/src/main/res/layout/fragment_explore.xml` - `card_current_source`
- **代码**：`ExploreFragment.showBookSourceSelector()`

### 3. 分组与分类筛选
- **功能**：分组信息通过BookSource.bookSourceGroup字段，分类通过ExploreKind
- **实现**：UI上分组和分类均以横向可滚动的Chip展示，点击后刷新对应分类和书籍列表
- **位置**：`app/src/main/res/layout/fragment_explore.xml` - `scroll_groups`, `scroll_categories`
- **代码**：`ExploreFragment.refreshGroupChips()`, `ExploreFragment.refreshCategoryChips()`

### 4. 数据流与刷新逻辑
- **功能**：所有筛选项（类型、书源、分组、分类）变更时，均需联动刷新下方数据
- **实现**：数据流在ExploreFragment统一管理，ViewModel负责数据加载，Adapter负责UI展示
- **代码**：`ExploreViewModel.getFilteredBookSources()`, `ExploreFragment.upExploreData()`

## 技术实现细节

### 新增的ViewModel功能
- `selectedSourceType`: 当前选中的书源类型
- `currentBookSource`: 当前选中的书源
- `selectedGroup`: 当前选中的分组
- `selectedCategory`: 当前选中的分类
- `getFilteredBookSources()`: 获取筛选后的书源列表
- `getAvailableGroups()`: 获取可用的分组列表

### 新增的Fragment功能
- `initSourceTypeTabs()`: 初始化书源类型切换标签
- `initCurrentSourceCard()`: 初始化当前书源信息卡片
- `refreshGroupChips()`: 刷新分组筛选芯片
- `refreshCategoryChips()`: 刷新分类筛选芯片
- `showBookSourceSelector()`: 显示书源选择器

### 新增的Adapter功能
- `filterByCategory()`: 根据分类筛选显示
- `getExpandedSource()`: 获取当前展开的书源
- `setExpandedState()`: 设置展开状态

## 文件修改清单

### 核心文件
1. **ExploreViewModel.kt** - 新增数据管理功能
2. **ExploreFragment.kt** - 新增UI逻辑和交互
3. **ExploreAdapter.kt** - 新增筛选和状态管理功能
4. **BookSource.kt** - 新增转换方法

### 布局文件
1. **fragment_explore.xml** - 新增UI组件布局

### 资源文件
1. **values/strings.xml** - 新增英文字符串资源
2. **values-zh/strings.xml** - 新增中文字符串资源

## 最小侵入原则

本次改造严格遵循最小侵入原则：
- ✅ 优先复用现有ExploreFragment.kt、ExploreViewModel.kt、ExploreCategoryAdapter.kt、ExploreBookAdapter.kt、BookSource.kt等核心模块
- ✅ 仅对UI和数据流做必要扩展，避免重复造轮子
- ✅ 保持原有功能完整性，新增功能作为扩展
- ✅ 使用现有的数据结构和API接口

## 使用说明

### 基本操作流程
1. 选择书源类型（小说/漫画/音频/文件）
2. 可选择特定书源或按分组筛选
3. 选择书源后可查看该书的分类
4. 点击分类可查看对应分类下的书籍
5. 支持"更多"跳转和展开/收起功能

### 筛选逻辑
- 书源类型变更 → 重置书源、分组、分类选择
- 书源变更 → 重置分类选择
- 分组变更 → 重置分类选择
- 分类变更 → 刷新书籍列表

## 注意事项

1. **性能考虑**：分类数据异步加载，避免阻塞UI
2. **错误处理**：网络异常时优雅降级，显示错误提示
3. **状态管理**：筛选条件变更时正确重置相关状态
4. **国际化**：支持中英文双语显示

## 后续优化建议

1. **缓存机制**：对分类数据进行本地缓存，提升加载速度
2. **搜索优化**：支持在分类内搜索书籍
3. **排序功能**：支持按热度、更新时间等排序
4. **个性化**：记住用户的筛选偏好设置
