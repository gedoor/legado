### AI 摘要功能配置指南 

本指南旨在帮助您理解并配置 Legado 的 AI 摘要功能。通过正确设置，您可以将兼容的第三方大语言模型（LLM）服务集成到应用中，实现对章节内容的自动化摘要。

---

#### **1. API 地址 (API URL)**

*   **说明：**
    此项用于指定 AI 服务提供商的接口端点（Endpoint）。它是应用程序发送请求以获取摘要结果的网络地址。

*   **常见获取方式：**
    *   登录您的 AI 服务提供商（如 OpenAI, Google AI, DeepSeek, Moonshot 等）的官方网站。
    *   进入其开发者或 API 文档页面，通常在 "API Reference" 或 "Endpoints" 部分可以找到。
    *   **例如 (DeepSeek):** 登录后，在 API 文档的“快速入门”部分通常会提供基础 URL。

*   **格式：**
    一个标准的 HTTPS URL 字符串。**暂时只支持openai规范的API。**

*   **示例：**
    *   **OpenAI:** `https://api.openai.com/v1/chat/completions`
    *   ~~**Google AI:** `https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent`~~
    *   **DeepSeek:** `https://api.deepseek.com/chat/completions`
    *   **Moonshot (月之暗面):** `https://api.moonshot.cn/v1/chat/completions`

---

#### **2. API 密钥 (API Key)**

*   **说明：**
    用于身份验证的安全凭证。**敏感信息，请妥善保管，切勿泄露。**
*   **注意：**
    **AI摘要功能基于openai规范开发**

*   **常见获取方式：**
    *   登录您的 AI 服务提供商网站。
    *   通常在个人账户设置、"API Keys" 或“访问令牌”菜单下。
    *   点击“创建新的 API 密钥”或类似按钮即可生成。
    *   **例如 (OpenAI/DeepSeek):** 登录后，点击右上角个人头像，在下拉菜单中选择 "API Keys" 或“API密钥管理”。

*   **格式：**
    一串由服务商提供的字母和数字组合的唯一字符串。

*   **示例：**
    *   **OpenAI 格式:** `sk-aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890AbCdEf`


---

#### **3. 模型名称 (Model)**

*   **说明：**
    指定您希望使用的具体 AI 模型。不同的模型在性能、处理速度、成本和摘要质量上有所差异。

*   **常见获取方式：**
    *   在 AI 服务商的 API 文档或“模型”页面查找。
    *   通常会有一个列表，详细说明每个模型的名称（Model ID）、能力和适用场景。
    *   直接复制您需要使用的模型 ID 即可。
    *   **例如 (deepseek):** 在其“平台文档”的“模型列表”页面，可以找到如 `deepseek-r1-0528`, `deepseek-v3-0324` 等模型 ID。

*   **格式：**
    由服务商定义的模型标识符（ID）字符串。

*   **示例：**
    *   **OpenAI:** `gpt-4o`, `gpt-5（:`
    *   **Google AI:** `gemini-2.5-pro`, `gemini-2.5-flash`
    *   **DeepSeek:** `deepseek-r1`, `deepseek-v3`

---

#### **4. 自定义提示词 (Prompt)**

*   **说明：**
    向 AI 下达指令的模板。您可以通过修改提示词来精确控制摘要的风格、长度和格式。模板中可以使用占位符`{text}`，程序会自动将其替换为当前章节的原文。

*   **常见获取方式：**
    *   此项无需外部获取，完全由您自定义。
    *   您可以根据自己的需求，结合不同模型的特点，编写最适合您的指令。可以参考网络上关于“Prompt Engineering”（提示词工程）的技巧来优化效果。

*   **格式：**
    一段包含 `{text}` 占位符的文本字符串。

*   **示例：**
    *   **通用简洁摘要:**
        `请为以下文本生成一个简洁、中立的摘要，概括其核心内容：
{text}`
    *   **要点列表式摘要:**
        `请将以下文章的核心观点提取出来，并以无序列表（bullet points）的形式呈现，每个要点不超过30字：
{text}`
    *   **指定口吻和读者对象的摘要:**
        `假如你是一位领域专家，请用通俗易懂的语言，为非专业人士总结下面这段文字的关键信息和结论：
{text}`

---

#### **5. 缓存目录 (Cache Directory)**

*   **说明：**
    用于存储 AI 生成的摘要文件的本地文件夹。设置后，所有摘要结果都会被保存为独立的 `.txt` 文件，方便日后查阅和管理。

*   **常见获取方式：**
    *   此项无需外部获取。
    *   在 Legado 应用的 AI 摘要设置界面，点击此选项。
    *   应用会弹出文件浏览器，您只需在手机存储中选择一个已存在的文件夹，或创建一个新文件夹并选中即可。

*   **格式：**
    一个有效的设备本地文件夹路径。

*   **示例：**
    *   `/storage/emulated/0/Download/LegadoSummaries/`
    *   `/storage/emulated/0/Documents/AI摘要/`