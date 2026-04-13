# 在阅读界面添加 Agents 入口

## 目标

在 Android 原生小说阅读场景中新增 `Agents` 能力入口，并提供一个统一的全屏 Agents 会话界面。该界面需要支持阅读上下文注入、历史会话、OpenAI 兼容协议调用、本地工具调用，以及用户可配置的模型与提示词参数。

本设计仅覆盖 App 原生端，不包含 `modules/web`。

## 范围

- 在阅读页新增 `Agents` 入口
- 在“我的”页新增 `Agents` 入口
- 两处入口进入同一个 Agents 页面
- 新增 Agents 会话页、历史会话抽屉、设置页
- 新增 OpenAI 协议请求链路
- 新增本地工具注册与执行链路
- 新增会话与消息持久化
- 新增 Agents 相关配置项

## 非目标

- 不支持 Anthropic、Gemini 或其他协议
- 不实现 Web 端 Agents
- 不做多协议切换器
- 不做用户确认式工具写入流程
- 不把 `bookUrl` 直接暴露给模型或工具参数

## 入口设计

### 阅读页入口

- 在阅读界面现有搜索按钮左侧新增 `Agents` 入口
- 入口位于阅读页现有顶部/悬浮操作区中，与搜索入口保持同层级

### “我的”页入口

- 在“我的”界面 `字典规则` 下方新增 `Agents` 入口
- 保持与 `pref_main.xml` 现有配置项风格一致

### 入口行为

- 两处入口均打开同一个全屏 `Agents` 页面
- 不区分不同入口的目标页面
- 入口差异只体现在会话上下文注入策略上

## 页面结构

### Agents 主页面

页面采用四段式结构：

- 顶部栏
- 会话内容区
- 底部输入区
- 左侧历史会话抽屉

### 顶部栏

- 左上角：历史会话按钮
- 中间：页面标题 `Agents`
- 右上角：设置按钮

从阅读页进入并且当前会话绑定书籍上下文时，标题区域下方显示弱提示：

- 当前书名
- 当前章节名

从“我的”页进入时不显示当前书籍上下文提示。

### 历史会话抽屉

- 通过左上角按钮展开
- 只负责浏览与切换历史会话
- 每个会话项展示：
  - 会话标题
  - 最近更新时间
  - 可选副标题，例如绑定书名
- 当前会话高亮
- 抽屉底部固定“新建会话”按钮

### 会话内容区

- 展示层按 `user turn` 聚合，而不是按单条消息平铺
- 一个 `turn` 由以下内容组成：
  - 一条 `user` 消息
  - 该轮关联的 `assistant` 内容
  - 该轮触发的 `tool` 卡片
  - 该轮中的 `retry`、`error`、`stopped` 状态提示
- 内部系统提示、上下文注入结果、运行时附加信息不作为公开消息 role 设计
- 上述隐藏信息应存为 hidden part 或 message metadata，不作为普通可见消息平铺展示
- 工具消息采用状态机，而不是只有 `tool_call` / `tool_result` 两段式：
  - `pending`
  - `running`
  - `completed`
  - `error`
- 工具执行过程与结果应可见，便于追踪

### 工具卡片状态与展示

- 工具卡片至少展示：
  - 标题，例如“读取第 12 章”
  - 状态，例如“执行中”“成功”“失败”
  - 元数据，例如章节索引、返回条数、是否还有下一页
- 工具卡片在 `running` 或 `retry` 阶段应有明显进行中状态
- 工具卡片在 `error` 状态下应展示明确失败原因，且结果仍保留在消息流中

### Turn 展示规则

- 同一轮中的工具卡片、错误卡片、自动重试提示、停止生成状态都归属于触发它们的用户消息
- 若一轮中存在多次 assistant 输出或多次工具调用，仍归并到同一个 `turn` 中展示
- 当出现下一条用户消息时，视为新一轮开始，不再把后续内容并入上一轮

### 空状态

从阅读页进入且无历史消息时：

- 显示当前书籍上下文提示卡片
- 提供建议问题，例如：
  - 总结当前章节
  - 解释本章重点
  - 查看这本书已加载章节
  - 为这本书添加规则

从“我的”页进入且无历史消息时：

- 显示通用空状态
- 引导用户搜索书架里的书、查看书信息、管理规则

### 底部区域

- 输入框
- 发送按钮
- 在输入框与“新建会话”操作附近显示当前会话的上下文数字
- 上下文数字只显示当前近似上下文值，例如 `12.4k`
- 视觉风格参考 `opencode` 的紧凑数字信息样式，但不显示百分比、进度环、分布详情或展开面板
- 上下文数字应保持弱强调，可快速扫读，但不抢占输入区主操作焦点
- 当当前会话尚无可用上下文统计时，显示占位值 `--`
- 页面底部保留明显的新建会话操作入口

## 会话与上下文设计

## 会话模型

每个会话至少包含：

- `sessionId`
- `title`
- `createdAt`
- `updatedAt`
- `lastMessagePreview`
- `contextSnapshot`
  - `entrySource`
    - `reader`
    - `my`
  - `bookId`，可空
  - `bookName`，可空
  - `chapterIndex`，可空
  - `chapterTitle`，可空
  - `customInstructionText`，可空
- `systemBlocksSnapshot`
  - 按顺序保存该会话创建时真正生效的 system blocks
  - 每项至少包含：
    - `key`
    - `content`

`contextSnapshot` 与 `systemBlocksSnapshot` 是历史会话上下文的唯一真相源。

- 重新进入历史会话时，必须恢复该会话自己保存的历史消息与冻结上下文
- 不允许使用当前阅读位置、当前章节或最新全局 prompt 改写旧会话上下文
- 若用户想切换书籍、章节或上下文，必须新建会话

消息模型建议采用 `MessageInfo + MessagePart[]` 结构，而不是单一 `content + tool字段` 的扁平结构。

### MessageInfo

`MessageInfo` 至少包含：

- `messageId`
- `sessionId`
- `role`
- `role` 仅允许：
  - `user`
  - `assistant`
- `createdAt`

消息关联规则：

- `user` 消息不包含 `parentMessageId`
- `assistant` 消息必须包含：
  - `parentMessageId`
  - 其值必须指向触发本轮的 `user.messageId`
- `assistant` 消息还至少包含：
  - `completedAt`，可空
  - `providerId`，可空
  - `modelId`，可空
  - `finishReason`，可空
    - `completed`
    - `aborted`
    - `error`
  - `error`，可空
  - `tokens`，可空
- `turn` 是展示层按 `user.messageId` 聚合出来的视图概念，不单独作为持久化真相源

### MessagePart

每条消息可包含多个 `MessagePart`，并按顺序保留真实生成与执行过程。

首版至少支持以下 part 类型：

- `text`
- `tool`
- `retry`
- `abort`
- `error_card`

采用 `parts[]` 的原因：

- 一条 assistant 消息可能同时包含文本、工具调用结果、重试提示、停止提示与错误提示
- 多个 part 能稳定表达“先工具、后文本”的顺序关系
- 一条消息内可自然容纳多次工具调用，不需要不断扩展顶层 nullable 字段
- 后续若增加 `reasoning`、`summary` 等能力，只需新增 part 类型，不必重构主消息结构

首版推荐规则：

- 用户每发送一次请求，创建一条 `user` 消息
- 同一轮的运行结果创建一条 `assistant` 消息，并通过 `parentMessageId` 指向该 `user` 消息
- 该轮中的文本、工具、重试、停止与最终错误都作为这条 `assistant` 消息的 `parts[]` 追加

### `text` part

- 用于承载 assistant 正常文本输出

### `tool` part

- 用于承载一次工具调用的完整生命周期
- `tool` part 至少包含：
  - `toolCallId`
  - `toolName`
  - `state`
    - `pending`
    - `running`
    - `completed`
    - `error`
  - `toolInput`
  - `title`，可空
  - `metadata`，可空
  - `output`，可空
  - `error`，可空
  - `startedAt`，可空
  - `finishedAt`，可空
  - `interrupted`，可空
- `interrupted` 仅用于标记工具在停止生成或中断流程中被打断，不单独新增第五种工具状态

### `retry` part

- 用于表达自动重试等待，不与普通错误混淆
- 至少包含：
  - `attempt`
  - `reason`
  - `nextRetryAt`

### `abort` part

- 用于表达用户主动停止生成后的最终停止结果
- 至少包含：
  - `requestedAt`
  - `finalizedAt`，可空
  - `reason`
    - `user_stop`
    - `stream_aborted`
    - `tool_interrupted`

### `error_card` part

- 仅用于向用户展示最终结构化错误
- 不用于表达自动重试与停止生成这类运行态事件

## 历史分页与渲染窗口

- 存储层仍保存完整的 `message + part`
- 会话时间线接口按 `user root` 分页，而不是按裸 `message` 分页
- 每页返回最近 `N` 个 `turn`
- 每个 `turn` 至少包含：
  - `user`
  - `assistant`，可空
  - `parts`
- 分页契约：
  - `limit`
  - `before`
  - `nextCursor`
- `before` 为不透明游标，只用于加载更旧历史，不允许客户端自行解析业务含义
- `before` 的内部编码固定为 `base64url(JSON.stringify({ createdAt, messageId }))`
- 排序键固定为：
  - `createdAt DESC`
  - `messageId DESC`
- 服务端翻页条件固定为：
  - `createdAt < cursor.createdAt`
  - 或 `createdAt = cursor.createdAt && messageId < cursor.messageId`
- 首屏默认仅加载并渲染最近 `10` 个 `turn`
- 用户向上滚动时，每次增量回填最近未渲染的 `8` 个 `turn`
- 当滚动位置接近顶部时，应预取更旧历史，减少明显等待
- 增量回填旧历史时应尽量保持滚动位置稳定，避免列表跳动
- 历史会话抽屉只加载会话摘要，不预取每个会话的完整消息历史

## 上下文指标与估算

- 会话页需要维护当前会话的上下文指标，用于驱动底部区域的上下文数字展示
- 指标首版至少包括：
  - `contextUsed`
- `contextUsed` 表示当前请求视角下的输入上下文占用，展示为紧凑数字
- `k` 单位展示规则建议为：
  - 小于 `1000` 时显示原值，例如 `824`
  - 大于等于 `1000` 时显示一位小数，例如 `12.4k`
- 上下文估算优先级：
  - 优先使用 provider 返回的 token usage 作为最近一次已确认统计
  - 在发起下一次请求前，允许基于当前会话冻结的 `systemBlocksSnapshot`、本地历史消息、工具定义与必要开销做近似估算
- 展示给用户的是“当前近似上下文占用”，目的是帮助用户感知当前会话规模，而不是作为精确计费依据

## 上下文快照规则

- 新建会话时必须同时冻结：
  - `contextSnapshot`
  - `systemBlocksSnapshot`
- 会话后续默认沿用自己的冻结快照
- 不随着全局阅读进度、当前章节、最新偏好项或最新系统模板自动漂移
- 打开历史会话时，必须优先恢复会话自己的冻结快照
- 如果用户需要在新书、新章节或新上下文下继续对话，必须新建会话
- 标题生成、结构化输出、诊断模式等特殊请求允许追加临时 `system block`
- 临时 `system block` 仅在本次请求生效，不回写 `systemBlocksSnapshot`

## 书籍标识规则

- 对模型和工具暴露的是 `bookId`
- 不直接暴露 `bookUrl`
- 应用内部可通过 `bookId -> 现有书籍记录` 的方式解析到真实数据层

## 双入口上下文注入

### 从阅读页进入

- 仅在新建会话时写入当前：
  - `bookId`
  - 书名
  - 当前章节索引
  - 当前章节名
- `contextSnapshot.entrySource = reader`
- `systemBlocksSnapshot` 中应包含当前书籍与章节上下文块

### 从“我的”页进入

- 仅在新建会话时注入“无书籍上下文”的快照
- `contextSnapshot.entrySource = my`
- `contextSnapshot.bookId` 为空
- 若 agent 需要目标书，需先通过书架工具获取

## 会话运行状态

- 会话状态至少包含：
  - `idle`
  - `busy`
  - `retry`
  - `stopping`
- `busy` 表示当前正在进行模型生成或工具调度
- `retry` 表示当前请求进入自动重试等待阶段
- `stopping` 表示用户已请求停止，但当前请求仍在执行收尾流程
- `retry` 状态至少记录：
  - `attempt`
  - `reason`
  - `nextRetryAt`
- 页面在 `busy`、`retry` 或 `stopping` 状态下都必须提供明确的状态反馈
- 页面在 `busy` 或 `retry` 状态下必须提供“停止生成”操作

## 系统提示词设计

系统提示词设计参考 `opencode`，采用多块拼接结构，而不是单段大提示词。

- 普通会话请求不得重新读取最新全局模板来改写旧会话
- 普通会话请求必须优先使用会话自己的 `systemBlocksSnapshot`
- App 在发送请求时应按冻结顺序将 `systemBlocksSnapshot` 映射为 system blocks

### 提示词块组成

1. `Base Agent Block`
- 定义阅读助手的基础身份与核心职责

2. `Runtime Environment Block`
- 提供运行时事实信息
- 例如入口来源、当前环境、当前是否绑定书籍上下文

3. `Session Context Block`
- 仅在从阅读页进入并绑定书籍时注入
- 提供当前书名、`bookId`、章节名、章节索引

4. `Tool Usage Policy Block`
- 说明工具使用规则
- 明确必须使用 `bookId`
- 禁止直接构造或猜测 `bookUrl`
- 说明“已加载章节”只代表本地已加载范围

5. `Capabilities And Limits Block`
- 说明 agent 能做什么与不能做什么

6. `User Custom Instruction Block`
- 追加用户自定义提示词
- 仅做附加，不覆盖系统底层规则

### 拼接顺序与优先级

- 固定系统块顺序必须为：
  1. `Base Agent Block`
  2. `Runtime Environment Block`
  3. `Session Context Block`
  4. `Tool Usage Policy Block`
  5. `Capabilities And Limits Block`
- 特殊模式下允许追加额外 system block，例如：
  - 标题生成
  - 结构化输出
  - 诊断或测试连接模式
- 额外 system block 只能追加在固定系统块之后
- `User Custom Instruction Block` 始终放在最后
- 用户自定义提示词只能追加，不能覆盖或替换前置系统块
- 若不同 block 存在冲突，以更靠前的系统块为准
- App 在运行时应以“system block 数组”组装请求，而不是先拼成一段长文本再混合处理
- 历史会话继续对话时，默认使用冻结的 `systemBlocksSnapshot`

## 工具设计

工具命名统一采用：`<resource>_<action>`。

动作命名统一使用：

- `list`
- `search`
- `read`
- `write`
- `edit`

工具风格参考 `opencode` 的资源操作习惯，强调：

- 原子化
- 显式参数
- 可组合
- 读写分离

### 工具注册结构

- 每个工具至少定义：
  - `name`
  - `description`
  - `parametersSchema`
  - `enabled`
  - `executor`
- `parametersSchema` 必须可直接序列化为 OpenAI-compatible function parameters
- 被禁用或当前上下文不可用的工具，不应发送给模型

### 工具说明文案规范

- 每个工具除 JSON Schema 外，还必须提供面向模型的操作说明文案
- 工具说明至少包括：
  - 该工具做什么
  - 何时应该使用该工具
  - 参数说明
  - 返回结构
  - 常见错误
  - 推荐调用顺序
- 涉及分页的工具必须明确 `offset`、`limit` 的语义与默认行为
- 涉及 `bookId`、章节索引、规则 id 的工具，必须明确禁止模型猜测或伪造标识
- 当应优先改用其他工具时，说明文案中应明确给出替代建议

## 书架书资源工具

### `bookshelf_book_list`

- 列出当前书架库中的书
- 首版“当前书架”定义为：全部在架书
- 支持 `offset`、`limit`

### `bookshelf_book_search`

- 在全部在架书中按书名、作者进行搜索
- 支持 `query`、`limit`

### `bookshelf_book_read`

- 输入 `bookId` 获取该书的详细信息
- 返回至少包含：
  - 书名
  - 作者
  - 简介
  - 阅读进度
  - 当前章节
  - 最新章节

## 已加载章节资源工具

### `loaded_chapter_list`

- 列出某本书本地已加载的章节
- 支持 `bookId`、`offset`、`limit`

### `loaded_chapter_search`

- 在某本书的已加载章节中搜索关键词
- 支持 `bookId`、`query`、`limit`

### `loaded_chapter_read`

- 读取指定 `bookId`、指定章节索引的已加载正文
- 支持 `offset`、`limit`
- 若章节未加载，必须明确返回错误

## 操作规则资源工具

### `operation_rule_list`

- 列出规则资源
- 支持按类型与范围浏览

### `operation_rule_search`

- 搜索规则资源
- 搜索范围至少包括：
  - 规则名称
  - 规则内容
  - 规则说明

### `operation_rule_read`

- 读取单条规则详情

### `operation_rule_write`

- 直接新增规则
- 不增加用户二次确认步骤

### `operation_rule_edit`

- 修改已有规则
- 支持基于已有内容进行局部替换

## 工具使用原则

- 不知道书时，先 `bookshelf_book_list` 或 `bookshelf_book_search`
- 获取 `bookId` 后，再 `bookshelf_book_read`
- 读取内容前，优先 `loaded_chapter_list`
- 搜索章节内容时，优先 `loaded_chapter_search`
- 修改规则前，优先 `operation_rule_search` 或 `operation_rule_read`
- 写规则和改规则可直接执行，不走确认流

## 工具返回结构

- 工具执行完成后，返回结果至少包含：
  - `title`
  - `metadata`
  - `output`
  - `attachments`，可选
- 工具执行失败时，仍需保留结构化失败结果，至少包含：
  - `title`
  - `metadata`
  - `error`
  - `startedAt`
  - `finishedAt`

## 模型调用链路

- 使用 OpenAI 兼容协议
- 请求结构采用标准：
  - `messages`
  - `tools`
  - `tool_calls`
- App 负责：
  - 读取当前会话冻结的 `systemBlocksSnapshot`
  - 组装 system blocks
  - 传入会话历史
  - 注册本地工具定义
  - 执行工具调用
  - 将工具执行结果序列化后回流给模型
  - 计算并更新当前会话上下文占用指标
- 普通会话消息请求默认使用会话自己的 `systemBlocksSnapshot`
- 标题生成、结构化输出、诊断模式等特殊请求允许追加 request-scoped system block
- request-scoped system block 不得回写 `systemBlocksSnapshot`

### 本地消息到 OpenAI 请求体映射

- `systemBlocksSnapshot` 映射为多个 `role=system` 消息
- 用户文本消息映射为 `role=user`
- assistant 文本回复映射为 `role=assistant`
- assistant 发起工具调用时，映射为 `role=assistant` 且带 `tool_calls[]`
- 每个 `tool_call` 至少包含：
  - `id`
  - `type=function`
  - `function.name`
  - `function.arguments`，使用 JSON 字符串
- 本地工具执行结果映射为 `role=tool`
- `role=tool` 消息必须带 `tool_call_id`，并与对应 `tool_call.id` 一一对应
- 工具输出若为结构化对象，需按统一规则序列化为字符串或 JSON 文本后再发送

### Tools 序列化规则

- 发送给 OpenAI-compatible 接口的工具定义统一序列化为：
  - `type=function`
  - `function.name`
  - `function.description`
  - `function.parameters`
- `function.parameters` 直接来源于本地工具 schema，不在请求前临时拼接自由文本参数说明

### Tool Choice 首版支持范围

- 首版支持以下 `toolChoice`：
  - `auto`
  - `none`
  - `required`
  - 指定单个工具名
- 默认使用 `auto`
- 若当前请求不允许使用工具，应显式传 `none`

## 自动重试与停止生成

- 对以下场景支持自动重试：
  - `429`
  - provider overload
  - 响应头显式提供 `retry-after` 或等价重试信息
- 自动重试期间，会话状态切换为 `retry`
- UI 中应明确展示“x 秒后自动重试”以及失败原因
- 自动重试采用退避策略，并优先遵循服务端返回的重试等待时间
- 用户可在 `busy` 或 `retry` 状态下点击“停止生成”取消本次请求
- 用户点击“停止生成”后，会话状态立即切换为 `stopping`
- 进入 `stopping` 后：
  - 立即停止继续接收新的模型输出
  - 取消尚未开始的自动重试
  - 禁止再发起新的工具调用
- 若当前没有工具在运行：
  - 当前 assistant 直接结束
  - `assistant.finishReason = aborted`
  - 追加 `abort` part
- 若当前有只读工具在运行：
  - 采用 best-effort cancel
  - 若成功中断：
    - `tool.state = error`
    - `tool.error = Tool execution aborted`
    - `tool.interrupted = true`
  - 若工具已自然完成：
    - 保留 `completed` 结果
    - 但之后不再允许模型基于该结果继续生成正文
- 若当前有写工具在运行，例如 `operation_rule_write/edit`：
  - 必须原子提交
  - 不允许留下部分写入
  - UI 保持 `stopping`，直到写工具完成 success 或 failure
  - 写工具结束后，本轮 assistant 直接结束，不再继续生成后续正文
- `abort` part 应在整轮收尾完成后再写入，而不是在用户点击停止的瞬间立即写入
- 自动重试失败后，需把最终错误以消息卡片形式落入消息流

## 设置项设计

设置页面采用项目现有 `Preference` 风格。

推荐结构：

- 新增 `pref_config_agents.xml`
- 新增 `AgentsConfigFragment`
- 通过默认偏好存储配置
- 公共读取逻辑可封装进 `AppConfig`

### 设置存取结构

- 每个 `agents*` 配置项都必须定义：
  - 持久化 key
  - 值类型
  - 默认值
  - 合法范围或校验规则
  - UI 控件类型
  - 是否脱敏显示
  - 是否即时生效
- UI 层不直接分散读取偏好项，应通过统一配置读取层暴露 getter / setter
- 建议新增统一配置入口，例如 `AgentsConfig` 或扩展 `AppConfig`

## 设置分组

### 连接设置

- `agentsBaseUrl`
  - 类型：`string`
  - 默认值：空
  - 校验：必须为合法 `http/https` 地址
  - 控件：文本输入
  - 脱敏：否
  - 生效时机：下次请求
- `agentsApiKey`
  - 类型：`string`
  - 默认值：空
  - 校验：允许为空，但发送请求前必须校验非空
  - 控件：密码输入
  - 脱敏：是
  - 生效时机：下次请求
- `agentsModel`
  - 类型：`string`
  - 默认值：空
  - 校验：非空字符串
  - 控件：文本输入或选择器
  - 脱敏：否
  - 生效时机：下次请求
- `agentsRequestTimeout`
  - 类型：`int`
  - 默认值：`60`
  - 校验：`5-600` 秒
  - 控件：数字输入
  - 脱敏：否
  - 生效时机：下次请求

### 模型参数

- `agentsTemperature`
  - 类型：`double`
  - 默认值：`1.0`
  - 校验：`0-2`
  - 控件：数字输入或滑杆
  - 脱敏：否
  - 生效时机：下次请求
- `agentsMaxOutputTokens`
  - 类型：`int`
  - 默认值：`4096`
  - 校验：`256-32768`
  - 控件：数字输入
  - 脱敏：否
  - 生效时机：下次请求
- 可选：`agentsTopP`
  - 类型：`double?`
  - 默认值：空，表示跟随 provider 默认值
  - 校验：`0-1`
  - 控件：数字输入
  - 脱敏：否
  - 生效时机：下次请求

### 提示词设置

- `agentsCustomInstruction`
  - 类型：`string`
  - 默认值：空
  - 校验：允许为空
  - 控件：多行文本输入
  - 脱敏：否
  - 生效时机：下次请求

### 工具设置

- `agentsEnableBookshelfTools`
  - 类型：`boolean`
  - 默认值：`true`
  - 控件：开关
  - 生效时机：下次请求
- `agentsEnableLoadedChapterTools`
  - 类型：`boolean`
  - 默认值：`true`
  - 控件：开关
  - 生效时机：下次请求
- `agentsEnableOperationRuleTools`
  - 类型：`boolean`
  - 默认值：`true`
  - 控件：开关
  - 生效时机：下次请求

### 会话设置

- `agentsAutoCreateTitle`
  - 类型：`boolean`
  - 默认值：`true`
  - 控件：开关
  - 生效时机：下次新建标题流程
- `agentsHistoryLimit`
  - 类型：`int`
  - 默认值：`200`
  - 校验：`20-1000`
  - 控件：数字输入
  - 生效时机：保存后立即影响历史查询上限
- `agentsClearHistory`
  - 类型：动作项
  - 默认值：无
  - 控件：危险操作按钮
  - 生效时机：立即执行

### 自动标题规则

- 仅在首个真实用户消息发送后触发自动标题生成
- 自动标题生成异步执行，不阻塞主回复返回
- 仅默认标题允许被自动替换
- 若用户已手动修改标题，后续不再自动覆盖
- 自动标题失败不影响主会话流程，也不应打断当前消息发送

### 设置辅助能力

- 提供轻量测试连接能力
- 用于验证 `baseUrl + apiKey + model` 是否可用

## 错误处理

## 基本原则

- 错误进入消息流，不只做临时弹窗
- 错误不应导致整个页面不可用
- 用户消息不因请求失败而丢失

## 模型请求错误

需覆盖：

- `baseUrl` 不可达
- `apiKey` 无效
- `model` 不存在
- 请求超时
- OpenAI 兼容响应异常

处理方式：

- 插入系统风格错误消息卡片
- 支持“重试本次请求”
- 配置错误时支持“去设置”
- 对可重试错误自动进入 `retry` 状态，并展示倒计时
- 用户可在倒计时期间手动停止自动重试

推荐标准错误码：

- `MODEL_BASE_URL_UNREACHABLE`
- `MODEL_AUTH_FAILED`
- `MODEL_NOT_FOUND`
- `MODEL_TIMEOUT`
- `MODEL_RESPONSE_INVALID`

## 工具执行错误

需覆盖：

- `bookId` 无效
- 章节未加载
- 规则不存在
- 规则写入或编辑失败
- 搜索结果为空

推荐标准错误码：

- `BOOK_NOT_FOUND`
- `CHAPTER_NOT_LOADED`
- `RULE_NOT_FOUND`
- `RULE_WRITE_FAILED`
- `RULE_EDIT_FAILED`
- `SEARCH_NO_RESULT`

处理方式：

- 工具失败时仍需返回结构化失败结果，而不是只抛弃本轮输出
- 明确标记失败原因
- assistant 可基于失败结果继续推理
- 失败工具卡片状态必须为 `error`，并保留输入、失败时间与必要元数据
- 失败结果至少应包含：
  - `error.code`
  - `error.message`
  - `error.retryable`

## 写工具失败边界

- `operation_rule_write` 输入非法时直接失败
- `operation_rule_edit` 未命中目标内容时直接失败
- 禁止模糊写入与半成品写入

## 实现建议

## 页面与入口

- 新增统一的 `Agents` 全屏页面
- 新增历史会话抽屉
- 新增 Agents 设置页
- 在阅读页与“我的”页接入入口

## 数据与持久化

- 新增会话与消息持久化
- 新增书籍 `bookId` 标识方案
- 新增工具执行层与工具注册表

## 配置与请求

- 新增 OpenAI 兼容客户端封装
- 新增 Agents 配置读取层
- 新增系统提示词分块组装器

## 验收标准

- 阅读页搜索按钮左侧存在 `Agents` 入口
- “我的”页 `字典规则` 下方存在 `Agents` 入口
- 两个入口进入同一个 Agents 页面
- 页面左上角可展开历史会话，右上角可进入设置，底部可新建会话
- 输入框与“新建会话”操作附近存在当前会话上下文数字显示
- 上下文数字显示风格参考 `opencode` 的紧凑数字样式，但不展示百分比、进度和 breakdown
- 会话内容区按 `user turn` 聚合展示，而不是按单条消息平铺
- 历史会话重新进入时，会恢复该会话自己保存的历史消息与冻结上下文，而不是套用当前阅读位置或最新系统提示词
- 新建会话时会冻结 `contextSnapshot` 与 `systemBlocksSnapshot`
- 工具执行过程在消息流中以状态化卡片展示，至少覆盖 `pending`、`running`、`completed`、`error`
- `retry` 与 `abort` 在消息模型中有显式 part 类型，不借用普通错误卡片表达
- 从阅读页新建会话时，系统提示词中包含当前书与当前章节上下文
- 从“我的”页进入时，系统提示词中不包含当前书 id
- 模型调用仅支持 OpenAI 兼容协议
- 本地消息与工具定义会被正确映射到 OpenAI-compatible `messages`、`tools`、`tool_calls`
- 历史消息支持基于 `before + limit + nextCursor` 的分页加载
- 历史时间线按 `user root` 分页，`before` 为 `base64url({createdAt,messageId})` 风格的不透明游标
- 请求中用户可以主动“停止生成”
- 停止生成时，若只读工具正在运行，会被尽力中断并落为 `interrupted error`
- 停止生成时，若写工具已经开始执行，则必须原子完成，不允许部分写入，且之后不再继续生成 assistant 正文
- `429` 或 provider overload 时支持自动重试，并显示倒计时
- 用户可以配置 `baseUrl`、`apiKey`、`model` 与核心模型参数
- 每个 `agents*` 配置项都有默认值、校验规则、控件类型与统一 getter / setter 读取方式
- `agentsAutoCreateTitle` 开启时，仅首个真实用户消息触发异步自动标题，且只替换默认标题
- 书籍工具支持：
  - `bookshelf_book_list`
  - `bookshelf_book_search`
  - `bookshelf_book_read`
- 章节工具支持：
  - `loaded_chapter_list`
  - `loaded_chapter_search`
  - `loaded_chapter_read`
- 规则工具支持：
  - `operation_rule_list`
  - `operation_rule_search`
  - `operation_rule_read`
  - `operation_rule_write`
  - `operation_rule_edit`
- 写规则和改规则可直接执行，不需要用户确认
- 网络错误、配置错误、工具错误都能在消息流中可见

## 建议验证用例

- 从阅读页打开 Agents，要求总结当前章节，能够成功读取已加载章节并返回结果
- 从“我的”页打开 Agents，要求搜索书架中的书，能够返回带 `bookId` 的候选结果
- 从阅读页创建会话后切换到其他章节，再重新进入该历史会话，看到的历史消息与上下文仍保持创建时冻结的书籍/章节语义
- 输入 `bookId` 获取书籍详细信息能够成功
- 搜索规则、读取规则、写入规则、编辑规则的链路可完整跑通
- 配置错误的 `apiKey` 后，请求失败时页面可见错误并可跳转设置
- 对未加载章节执行读取时，返回明确失败结果而不是虚构内容
- 工具执行时，消息流中的工具卡片会经历 `pending -> running -> completed` 或 `pending -> running -> error`
- 返回 `429` 或 overload 时，页面显示“x 秒后自动重试”，且用户可手动停止
- 返回 `429` 或 overload 时，消息模型中会落下显式 `retry` part，而不是仅有临时 UI 状态
- 首个真实用户消息发送后，默认标题会异步更新；若用户手动改过标题，则不会被自动覆盖
- 同一轮中若发生多次工具调用与一次最终回答，UI 仍归并在同一个 `turn` 下展示
- 首屏仅渲染最近 `10` 个 `turn`，向上滚动时按批次回填旧历史且滚动位置稳定
- OpenAI-compatible 请求中，assistant 工具调用被编码到 `tool_calls`，工具结果被编码为 `role=tool`
- 会话存在可用上下文统计时，输入框与“新建会话”附近显示类似 `12.4k` 的紧凑数字
- 停止生成时，如果只读工具尚未完成，该工具最终落为 `error + interrupted=true`
- 停止生成时，如果写规则工具已经开始执行，最终只会出现“完整成功”或“完整失败”，不会出现半写入结果
