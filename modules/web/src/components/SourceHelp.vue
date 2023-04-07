<script setup>
import { Link } from "@element-plus/icons-vue";
</script>
<template>
  <el-link
    :icon="Link"
    href="https://alanskycn.gitee.io/teachme/"
    target="_blank"
    >书源制作教程</el-link
  ><br />
  <el-link
    :icon="Link"
    href="https://zhuanlan.zhihu.com/p/29436838"
    target="_blank"
    >xpath基础教程</el-link
  ><br />
  <el-link
    :icon="Link"
    href="https://zhuanlan.zhihu.com/p/32187820"
    target="_blank"
    >xpath高级教程</el-link
  ><br />
  <el-link
    :icon="Link"
    href="https://www.w3cschool.cn/regex_rmjc"
    target="_blank"
    >正则表达式教程</el-link
  ><br />
  <el-link :icon="Link" href="https://regexr-cn.com/" target="_blank"
    >正则表达式在线验证工具</el-link
  ><br />
  <div style="margin-top: 20px">
    <span
      ><el-text
        ><code>^$()[]{}.?+*|</code> 这些是Java正则特殊符号,匹配需转义</el-text
      ></span
    ><br />
    <span
      ><el-text><code>(?s)</code> 前缀表示跨行解析</el-text></span
    ><br />
    <span
      ><el-text><code>(?m)</code> 前缀表示逐行匹配</el-text></span
    ><br />
    <span
      ><el-text><code>(?i)</code> 前缀表示忽略大小写</el-text></span
    ><br />
  </div>
</template>

<style lang="scss" scoped>
.el-link {
  padding: 4px;
}
.el-text {
  padding-top: 20px;
}
</style>
