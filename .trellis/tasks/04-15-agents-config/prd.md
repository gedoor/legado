# Agents: 设置页与配置读取

## 背景

父任务：`.trellis/tasks/04-12-add-agents-entry/`。

本子任务实现 Agents 相关配置项的 Preference 设置页，并提供统一的配置读取入口，遵循项目现有 `ConfigActivity + XxxConfigFragment + pref_config_xxx.xml` 约定。

## 需求

1. 新增 Agents 设置页
- 新增 `pref_config_agents.xml`
- 新增 `AgentsConfigFragment`（放置在与现有 config fragment 一致的包路径下）

2. 配置项（首版）
至少包含以下 key（命名以 `agents*` 开头，具体 key 命名需与项目 PreferKey/AppConfig 体系一致）：
- 连接：`agentsBaseUrl`、`agentsApiKey`、`agentsModel`、`agentsRequestTimeout`
- 模型参数：`agentsTemperature`、`agentsMaxOutputTokens`
- 提示词：`agentsCustomInstruction`
- 工具开关：`agentsEnableBookshelfTools`、`agentsEnableLoadedChapterTools`、`agentsEnableOperationRuleTools`
- 会话：`agentsAutoCreateTitle`、`agentsHistoryLimit`

3. 校验规则
- `agentsBaseUrl`：必须为合法 `http/https` URL（允许为空但在发起请求前必须校验非空，由后续调用方负责）
- `agentsRequestTimeout`：`5-600` 秒
- `agentsTemperature`：`0-2`
- `agentsMaxOutputTokens`：`256-32768`
- `agentsHistoryLimit`：`20-1000`

4. 统一读取入口
在现有配置读取层（例如 `AppConfig` 或新增 `AgentsConfig`）提供清晰的 getter/setter 或属性封装，避免 UI 层到处散落直接读写 SharedPreferences。

5. 设置页入口
在“其他设置”（`OtherConfigFragment`）中新增一个入口项可进入 Agents 设置页（遵循现有 `ConfigActivity + ConfigTag` 跳转方式）。

## 非目标（本子任务不做）

- 不实现真实的“测试连接”网络请求（由 openai-client 子任务或后续集成任务实现）
- 不把设置项接入到 Agents 会话页（由后续集成任务实现）

## 验收标准

- [ ] 存在新的 `AgentsConfigFragment + pref_config_agents.xml`，能正常打开并展示配置项。
- [ ] 在“我的”页进入“设置/其他设置”后，可通过新增入口进入 Agents 设置页。
- [ ] 配置项具备合理默认值与校验（非法值不写入或给出明确提示），不导致崩溃。
- [ ] 存在统一的配置读取入口（`AppConfig` 扩展或新 `AgentsConfig`），调用方可不直接触碰 SharedPreferences。
