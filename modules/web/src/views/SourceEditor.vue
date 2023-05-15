<template>
  <div class="editor">
    <source-tab-form class="left" :config="config" />
    <tool-bar />
    <source-tab-tools class="right" />
  </div>
</template>
<script setup>
import bookSourceConfig from "@/config/bookSourceEditConfig";
import rssSourceConfig from "@/config/rssSourceEditConfig";
import "@/assets/main.css";

let config;

if (/bookSource/i.test(location.href)) {
  config = bookSourceConfig;
  document.title = "书源管理";
} else {
  config = rssSourceConfig;
  document.title = "订阅源管理";
}
</script>
<style lang="scss" scoped>
.editor {
  display: flex;
  height: 100vh;
  overflow: hidden;
  .left {
    flex: 1;
    margin-left: 20px;
  }
  .right {
    flex: 1;
    width: 360px;
    margin-right: 20px;
  }
}
</style>
