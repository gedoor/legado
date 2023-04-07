export default {
  base: {
    name: "基础",
    children: [
      {
        title: "源域名",
        id: "sourceUrl",
        type: "String",
        hint: "通常填写网站主页,例: https://www.qidian.com",
        required: true,
      },
      {
        title: "图标",
        id: "sourceIcon",
        type: "String",
        hint: "填写图片网络链接",
      },
      {
        title: "源名称",
        id: "sourceName",
        type: "String",
        hint: "会显示在源列表",
        required: true,
      },
      {
        title: "源分组",
        id: "sourceGroup",
        type: "String",
        hint: "描述源的特征信息",
      },
      {
        title: "源注释",
        id: "sourceComment",
        type: "String",
        hint: "描述源作者和状态",
      },
      {
        title: "分类地址",
        id: "sortUrl",
        type: "String",
        hint: "名称1::链接1\n名称2::链接2",
      },
      {
        title: "登录地址",
        id: "loginUrl",
        type: "String",
        hint: "填写网站登录网址,仅在需要登录的源有用",
      },
      {
        title: "登录界面",
        id: "loginUi",
        type: "String",
        hint: "自定义登录界面",
      },
      {
        title: "登录检测",
        id: "loginCheckJs",
        type: "String",
        hint: "登录检测js",
      },
      {
        title: "封面解密",
        id: "coverDecodeJs",
        type: "String",
        hint: "封面解密js",
      },
      {
        title: "请求头",
        id: "header",
        type: "String",
        hint: "客户端标识",
      },
      {
        title: "变量说明",
        id: "variableComment",
        type: "String",
        hint: "源变量说明",
      },
      {
        title: "并发率",
        id: "concurrentRate",
        type: "String",
        hint: "并发率",
      },
    ],
  },
  list: {
    name: "列表",
    children: [
      {
        title: "列表规则",
        id: "ruleArticles",
        type: "String",
        hint: "规则结果为List<Element>",
      },
      {
        title: "翻页规则",
        id: "ruleNextPage",
        type: "String",
        hint: "下一页链接 规则结果为List<String>或String",
      },
      {
        title: "标题规则",
        id: "ruleTitle",
        type: "String",
        hint: "文章标题 规则结果为String",
      },
      {
        title: "时间规则",
        id: "rulePubDate",
        type: "String",
        hint: "文章发布时间 规则结果为String",
      },
      {
        title: "描述规则",
        id: "ruleDescription",
        type: "String",
        hint: "文章简要描述 规则结果为String",
      },
      {
        title: "图片规则",
        id: "ruleImage",
        type: "String",
        hint: "文章图片链接 规则结果为String",
      },
      {
        title: "链接规则",
        id: "ruleLink",
        type: "String",
        hint: "文章链接 规则结果为String",
      },
    ],
  },
  webView: {
    name: "WebView",
    children: [
      {
        title: "内容规则",
        id: "ruleContent",
        type: "String",
        hint: "文章正文",
      },
      {
        title: "样式规则",
        id: "style",
        type: "String",
        hint: "文章正文样式 填写css",
      },
      {
        title: "注入规则",
        id: "injectJs",
        type: "String",
        hint: "注入网页的JavaScript",
      },
      {
        title: "黑名单",
        id: "contentBlacklist",
        type: "String",
        hint: "webView链接加载黑名单，英文逗号隔开",
      },
      {
        title: "白名单",
        id: "contentWhitelist",
        type: "String",
        hint: "webView链接加载白名单，英文逗号隔开",
      },
    ],
  },
  other: {
    name: "其他",
    children: [
      {
        title: "列表样式",
        id: "articleStyle",
        type: "Array",
        array: ["默认", "大图", "双列"],
      },
      {
        title: "加载地址",
        id: "loadWithBaseUrl",
        type: "Boolean",
      },
      {
        title: "启用JS",
        id: "enableJs",
        type: "Boolean",
      },
      {
        title: "启用",
        id: "enabled",
        type: "Boolean",
      },
      {
        title: "Cookie",
        id: "enabledCookieJar",
        type: "Boolean",
      },
      {
        title: "单URL",
        id: "singleUrl",
        type: "Boolean",
      },
      {
        title: "排序编号",
        id: "customOrder",
        type: "Number",
      },
    ],
  },
};
